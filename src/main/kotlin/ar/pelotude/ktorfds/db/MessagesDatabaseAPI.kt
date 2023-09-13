package ar.pelotude.ktorfds.db

import ar.pelotude.ktorfds.models.GeoMessage
import ar.pelotude.ktorfds.models.Location
import ar.pelotude.ktorfds.msgs.Message

interface MessagesDatabase<T> {
    suspend fun getMessage(id: Long): GeoMessage<T>?

    suspend fun findMessages(
        origin: Location?,
        since: Long?,
        vararg id: Long?,
    ): Collection<GeoMessage<T>>

    suspend fun addMessage(
        userId: Long,
        location: Location,
        content: T,
    ): GeoMessage<T>?

    suspend fun addMessage(
        userId: Long,
        location: Location,
        message: Message<T, *>,
    ) = addMessage(userId, location, message.value)

    suspend fun deleteMessage(id: Long): Boolean

    suspend fun vote(
        messageId: Long,
        userId: Long,
        vote: Vote,
    )
}

enum class Vote {
    LIKE,
    DISLIKE,
    UNSET,
}