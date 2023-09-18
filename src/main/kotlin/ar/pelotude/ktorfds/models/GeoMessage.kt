package ar.pelotude.ktorfds.models

import kotlinx.serialization.Serializable

@Serializable
data class Location(
    val lat: Double,
    val long: Double,
    val level: Int,
)

data class GeoMessage<T>(
    val id: Long,
    val location: Location,
    val content: T,
    val likes: Long,
    val dislikes: Long,
    val creationTime: Long,
    val authorId: Long,
    val deleted: Boolean,
    val likedByRequester: Int = 0,
)

@Serializable
data class PublicGeoMessage<T>(
    val id: Long,
    val location: Location,
    val content: T,
    val likes: Long,
    val dislikes: Long,
    val liked: Int,
    val creationTime: Long,
    val bySelf: Boolean,
)

// TODO: the db already handles "likedByRequested"
//  so it should do the same for "(created) bySelf"
fun <T> GeoMessage<T>.toPublicGeoMessage(recipientId: Long) = PublicGeoMessage(
    id,
    location,
    content,
    likes,
    dislikes,
    likedByRequester,
    creationTime,
    bySelf = authorId == recipientId,
)