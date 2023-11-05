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
    val rated: Int = 0,
)

@Serializable
data class PublicGeoMessage<T>(
    val id: Long,
    val location: Location,
    val content: T,
    val likes: Long = 0,
    val dislikes: Long = 0,
    val rated: Int = 0,
    val creationTime: Long,
    val bySelf: Boolean = false,
)

// TODO: the db already handles "likedByRequester"
//  so it should do the same for "(created) bySelf"
fun <T> GeoMessage<T>.toPublicGeoMessage(recipientId: Long) = PublicGeoMessage(
    id = id,
    location = location,
    content = content,
    likes = likes,
    dislikes = dislikes,
    rated = rated,
    creationTime = creationTime,
    bySelf = authorId == recipientId,
)