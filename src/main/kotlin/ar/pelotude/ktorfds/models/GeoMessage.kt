package ar.pelotude.ktorfds.models

import kotlinx.serialization.Serializable

@Serializable
data class Location(
    val lat: Double,
    val long: Double,
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
)

@Serializable
data class PublicGeoMessage<T>(
    val id: Long,
    val location: Location,
    val content: T,
    val likes: Long,
    val dislikes: Long,
    val creationTime: Long,
    val bySelf: Boolean = false
)

fun <T> GeoMessage<T>.toPublicGeoMessage(recipientId: Long) = PublicGeoMessage(
    id,
    location,
    content,
    likes,
    dislikes,
    creationTime,
    authorId == recipientId,
)