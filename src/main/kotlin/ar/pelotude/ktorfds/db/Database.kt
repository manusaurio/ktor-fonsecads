package ar.pelotude.ktorfds.db

import ar.pelotude.ktorfds.models.GeoMessage
import ar.pelotude.ktorfds.models.Location
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.sqlite.SQLiteConfig
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException

class Database(val dbUrl: String): MessagesDatabase<Long> {
    private val config = SQLiteConfig().apply {
        enforceForeignKeys(true)
        enableLoadExtension(true)
        setJournalMode(SQLiteConfig.JournalMode.WAL)
        busyTimeout = 10_000
    }.toProperties()

    private fun Connection.createSchema() = createStatement().use { statement ->
        sequenceOf(
            "SELECT load_extension('mod_spatialite');",
            "SELECT InitSpatialMetaData();",
            """
            CREATE TABLE message(
              id INTEGER NOT NULL PRIMARY KEY,
              author_id INTEGER NOT NULL,
              content INTEGER NOT NULL,
              deleted INTEGER NOT NULL DEFAULT 0,
              creation_time INTEGER NOT NULL DEFAULT(UNIXEPOCH()),
              level INTEGER NOT NULL DEFAULT 0,
              FOREIGN KEY(author_id) REFERENCES user(id)
            );
            """,
            "SELECT AddGeometryColumn('message', 'coordinates', 4326, 'POINT', 'XY');",
            """
            CREATE TABLE user(
              id INTEGER NOT NULL PRIMARY KEY,
              creation_date INTEGER NOT NULL DEFAULT(UNIXEPOCH()),
              banned INTEGER DEFAULT 0
            );
            """,
            """
            CREATE TABLE like(
              message_id INTEGER NOT NULL,
              author_id INTEGER NOT NULL,
              grade INTEGER NOT NULL, -- -1, 0 or 1
              UNIQUE(message_id, author_id),
              FOREIGN KEY(message_id) REFERENCES message(id),
              FOREIGN KEY(author_id) REFERENCES user(id)
            );
            """,
            // idk if I'm gonna use these tables but just in case:
            """
            CREATE TABLE message_part(
              type INTEGER NOT NULL, -- 0 template 1 filling 2 conjunction
              content TEXT NOT NULL,
              value INTEGER NOT NULL,
              filling_category_id INTEGER,
              UNIQUE(type, content, value),
              PRIMARY KEY(type, value),
              FOREIGN KEY(filling_category_id) REFERENCES filling_cateogry(id)
            );
            """,
            """
            CREATE TABLE filling_category(
              id INTEGER NOT NULL PRIMARY KEY,
              name TEXT UNIQUE NOT NULL
            );
            """
        ).forEach(statement::execute)

        if (!autoCommit) commit()
    }

    // this prevents repetition, but it's a bit of a mess if I want to change it later...
    private val unconditionalSelectSqlTemplate = """SELECT id, m.author_id, content, deleted,
           ST_X(coordinates) as x, ST_Y(coordinates) as y, level,
           creation_time,
           COALESCE((SELECT grade FROM like WHERE m.id = like.message_id AND like.author_id = %s), 0) as liked_by_requester,
           SUM(CASE WHEN grade = 1 THEN 1 ELSE 0 END) AS likes,
           SUM(CASE WHEN grade = -1 THEN 1 ELSE 0 END) AS dislikes
            FROM message m
            LEFT JOIN like
              ON m.id = like.message_id %s
            GROUP BY m.id
            LIMIT %s""".trimMargin()

    private inline fun <T> ResultSet.toGeoMessage(
        crossinline contentTransformer: ResultSet.(columnLabel: String) -> T,
    ): GeoMessage<T> = GeoMessage(
        id = getLong("id"),
        location = Location(getDouble("x"), getDouble("y"), getInt("level")),
        content = contentTransformer("content"),
        likes = getLong("likes"),
        dislikes = getLong("dislikes"),
        creationTime = getLong("creation_time"),
        authorId = getLong("author_id"),
        deleted = getLong("deleted") != 0L,
        rated = getInt("liked_by_requester"),
    )

    private val writingMutex = Mutex()

    // TODO: use HikariCP or something proper to handle connections
    private val connection = DriverManager.getConnection(
        dbUrl,
        config,
    ).apply {
        autoCommit = false
        createStatement().executeUpdate("SELECT load_extension('mod_spatialite');")
        commit() // Not sure if the spatialite function requires a commit... it shouldn't
    }

    /** Connection for read-only requests - autocommit on */
    private val readingConnection = DriverManager.getConnection(
        dbUrl,
        config,
    ).apply {
        autoCommit = true
        createStatement().executeUpdate("SELECT load_extension('mod_spatialite');")
    }

    private suspend inline fun <T: Any?> writingTransaction(
        crossinline block: CoroutineScope.(conn: Connection) -> T,
    ): T = writingMutex.withLock {
        return withContext(Dispatchers.IO) {
            this.block(connection)
        }
    }

    private suspend inline fun <T: Any?> readingTransaction(
        crossinline block: CoroutineScope.(conn: Connection) -> T,
    ): T {
        return withContext(Dispatchers.IO) {
            this.block(readingConnection)
        }
    }

    init {
        try {
            DriverManager.getConnection(dbUrl, config)?.use { conn ->
                conn.autoCommit = false

                conn.createStatement().use { s ->
                    val res = s.executeQuery("PRAGMA user_version;")
                    conn.commit()

                    val userVersion = res.getInt(1)

                    if (userVersion == 0) {
                        conn.createSchema()
                        s.executeUpdate("PRAGMA user_version = 1;")
                        conn.commit()
                    }
                }
            }
        } catch (e: SQLException) {
            // TODO:
            throw e
        }
    }

    override suspend fun addMessage(
        userId: Long,
        location: Location,
        content: Long,
    ): GeoMessage<Long>? = writingTransaction { conn ->
        // TODO: Remove this insertion and add hook
        var userInsert: PreparedStatement? = null
        var messageInsert: PreparedStatement? = null

        return@writingTransaction try {
            userInsert = conn.prepareStatement(
                "INSERT OR IGNORE INTO user(id) VALUES (?);"
            ).apply {
                setLong(1, userId)
                executeUpdate()
            }

            messageInsert = conn.prepareStatement(
                "INSERT INTO message(author_id, content, coordinates, level) " +
                        "VALUES (?, ?, GeomFromText('POINT(' || ? || ' ' || ? || ')', 4326), ?);"
            ).apply {
                setLong(1, userId)
                setLong(2, content)
                setString(3, location.lat.toString())
                setString(4, location.long.toString())
                setInt(5, location.level)
                executeUpdate()
            }

            val res = conn.createStatement().use { s ->
                s.executeQuery(
                    unconditionalSelectSqlTemplate.format(userId, "WHERE id = last_insert_rowid()", "1"),
                ).toGeoMessage(ResultSet::getLong)
            }

            conn.commit()

            res
        } catch (e: SQLException) {
            e.printStackTrace()
            conn.rollback()
            null
        } finally {
            userInsert?.close()
            messageInsert?.close()
        }
    }

    override suspend fun getMessage(id: Long, requesterId: Long): GeoMessage<Long>? = readingTransaction { conn ->
        conn.prepareStatement(
            unconditionalSelectSqlTemplate.format("?", "WHERE id = ? AND deleted = 0", "1")
        ).use { s ->
            s.setLong(1, requesterId)
            s.setLong(2, id)
            val rs = s.executeQuery()
            // conn.commit()

            if (rs.next()) rs.toGeoMessage(ResultSet::getLong) else null
        }
    }

    override suspend fun findMessages(
        requesterId: Long,
        origin: Location?,
        maxDistance: Double?,
        since: Long?,
        limit: Int,
        vararg ids: Long,
    ): Collection<GeoMessage<Long>> {
        // I wonder if there's any benefit using a prepared statement over this in this case...
        val idsCondition: String? = ids.takeIf { ids.isNotEmpty() }?.run {
            joinToString(separator = ",", prefix = "id in (", postfix = ")")
        }

        val distanceCondition = if (maxDistance != null && origin != null) {
            """ST_Distance(
              ST_Transform(coordinates, 5348),
              ST_Transform(ST_GeomFromText('POINT(${origin.long} ${origin.lat})', 4326), 5348)
            ) <= $maxDistance AND level = ${origin.level}""".trimMargin() // 5348?
        } else null

        val sinceCondition = since?.let {
            "m.creation_time > $since"
        }

        val condition = listOfNotNull(
            idsCondition,
            distanceCondition,
            sinceCondition,
        ).takeIf { it.isNotEmpty() }
            ?.joinToString(" AND ", prefix = "WHERE ")
            ?: ""

        return readingTransaction { conn ->
            conn.createStatement().use { s ->
                val rs = s.executeQuery(unconditionalSelectSqlTemplate.format(requesterId, condition, limit.toString()))

                generateSequence {
                    if (rs.next()) {
                        rs.toGeoMessage(ResultSet::getLong)
                    } else null
                }.toList()
            }
        }
    }

    override suspend fun vote(messageId: Long, userId: Long, vote: Vote): Boolean = writingTransaction { conn ->
        conn.prepareStatement(
            """INSERT INTO like(message_id, author_id, grade) VALUES(?, ?, ?)
            ON CONFLICT (message_id, author_id) DO
              UPDATE SET grade=excluded.grade WHERE message_id = excluded.message_id;""".trimMargin()
        ).use { statement ->
            return@use try {
                with(statement) {
                    setLong(1, messageId)
                    setLong(2, userId)
                    setInt(3, vote.value)
                    executeUpdate()
                    conn.commit()
                }
                true
            } catch (e: SQLException) {
                if (e.errorCode == 787) { // (787) SQLITE_CONSTRAINT_FOREIGNKEY
                    // TODO: Use a thread-safe logger
                    System.err.println("Someone attempted to vote a non-existant message: mid $messageId uid $userId")
                } else e.printStackTrace()
                false
            }
        }
    }

    override suspend fun deleteMessage(id: Long): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun createUser(): Long? = writingTransaction { conn ->
        return@writingTransaction try {
            val newId = conn.createStatement().use { s ->
                s.executeQuery("INSERT INTO user DEFAULT VALUES RETURNING id;").use { rs ->
                    rs.getLong(1)
                }
            }

            conn.commit()
            newId
        } catch (e: SQLException) {
            // TODO: handle rollback
            e.printStackTrace()
            null
        }
    }
}