package ar.pelotude.ktorfds.routes

import ar.pelotude.ktorfds.db.MessagesDatabase
import ar.pelotude.ktorfds.db.Vote
import ar.pelotude.ktorfds.models.PublicGeoMessage
import ar.pelotude.ktorfds.models.toPublicGeoMessage
import ar.pelotude.ktorfds.msgs.Messenger
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.get as koinGet


// TODO: Move
@Serializable
data class ErrorResponse(val error: String)

// TODO: Move. Add remaining fields
@Serializable
data class UserCreationResponse(val id: Long)

// TODO: Replace with something better
private fun PipelineContext<Unit, ApplicationCall>.getRequesterIdOrNull(): Long? {
    return call.request.cookies["userId"]?.toLongOrNull()?.takeIf { it > 0 }
}

// TODO: Intercept in pipeline
private val ApplicationRequest.comesFromFetch: Boolean
    get() = this.header("X-Requested-With").equals("fetch")

private suspend fun PipelineContext<Unit, ApplicationCall>.respondAuthRequired() =
    call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Authentication required"))

private suspend fun PipelineContext<Unit, ApplicationCall>.respondXRequestedWithRequired() =
    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing required header or invalid value: X-Requested-With"))

fun Route.messagesRouting() {
    val devMode = (environment?.config?.propertyOrNull("ktor.environment")
        ?: "prod") == "dev"

    val database = koinGet<MessagesDatabase<Long>>()

    val messenger = koinGet<Messenger<Long, *>>()

    val invalidArgumentsRsp = ErrorResponse("Invalid arguments.")

    post("/login") {
        if (!call.request.comesFromFetch) {
            return@post respondXRequestedWithRequired()
        }

        database.createUser()?.let { newId ->
            val userIdCookie = Cookie(
                name = "userId",
                value = newId.toString(),
                path = "/",
                httpOnly = true,
                secure = !devMode,
            )

            call.response.cookies.append(userIdCookie)

            call.respond(HttpStatusCode.Created, UserCreationResponse(newId))
        } ?: call.respond(HttpStatusCode.InternalServerError, "Something went wrong...")
    }

    // TODO: proper auth handling
    route("/messages") {
        get { // get message
            val requesterId: Long = getRequesterIdOrNull()
                ?: return@get respondAuthRequired()

            if (!call.request.comesFromFetch) {
                return@get respondXRequestedWithRequired()
            }

            val ids: Set<Long> = call.parameters["ids"]
                ?.takeIf { it.length < 200 }
                ?.split('.')
                ?.mapNotNull(String::toLongOrNull)
                ?.toSet() ?: setOf()

            val since = call.parameters["since"]?.toLongOrNull()

            val location = call.parameters["location"]?.toLocationOrNull()

            val radius = call.parameters["radius"]?.toDoubleOrNull()?.takeIf {
                it.isFinite() && it > 0.0
            }

            val messages: List<PublicGeoMessage<Long>>? = when {
                location == null && since == null && ids.size == 1 -> database.getMessage(ids.first(), requesterId)
                    ?.toPublicGeoMessage(requesterId)
                    ?.let(::listOf)
                    ?: listOf()

                location != null -> database.findMessages(
                    requesterId = requesterId,
                    origin = location,
                    maxDistance = radius,
                    since = since,
                    ids = ids.toLongArray(),
                ).map { it.toPublicGeoMessage(requesterId) }

                else -> null
            }

            if (messages != null) {
                call.respond<List<PublicGeoMessage<Long>>>(messages)
            } else {
                call.respond(HttpStatusCode.BadRequest, invalidArgumentsRsp)
            }
        }

        post { // post message
            val requesterId: Long = getRequesterIdOrNull()
                ?: return@post respondAuthRequired()

            if (!call.request.comesFromFetch) {
                return@post respondXRequestedWithRequired()
            }

            val (location, content) = call.receiveParameters().let {
                it["location"]?.toLocationOrNull() to it["content"]?.toLongOrNull()
            }

            if (
                location != null
                && content != null && messenger.quickCheck(content)
                ) {
                val posted = database.addMessage(
                    userId = requesterId,
                    location = location,
                    content = content,
                )?.toPublicGeoMessage(requesterId)

                if (posted != null) {
                    call.respond(
                        HttpStatusCode.Created,
                        posted,
                    )
                } else call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Something went wrong."))
            } else call.respond(HttpStatusCode.BadRequest, invalidArgumentsRsp)
        }

        post("/vote") {
            val requesterId: Long = getRequesterIdOrNull()
                ?: return@post respondAuthRequired()

            if (!call.request.comesFromFetch) {
                return@post respondXRequestedWithRequired()
            }

            val (id, vote) = call.receiveParameters().let {
                it["id"]?.toLongOrNull()?.takeIf { n -> n > 0 } to it["vote"]?.toIntOrNull()?.let(Vote::fromInt)
            }

            if (id != null && vote != null) {
                val res = database.vote(
                    messageId = id,
                    userId = requesterId,
                    vote = vote,
                )

                if (res) call.respond(HttpStatusCode.OK)
                else call.respond(HttpStatusCode.BadRequest, ErrorResponse("Your request could not be processed"))
            } else call.respond(HttpStatusCode.BadRequest, invalidArgumentsRsp)
        }
    }
}