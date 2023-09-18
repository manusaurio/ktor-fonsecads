package ar.pelotude.ktorfds.routes

import ar.pelotude.ktorfds.models.Location


fun String.toLocationOrNull(): Location? {
    val parts = this
        .trim()
        .split("\\s+".toRegex(), 3)

    if (parts.size == 3) {
        val latitude = parts[0].toDoubleOrNull()?.takeIf(Double::isFinite)
        val longitude = parts[1].toDoubleOrNull()?.takeIf(Double::isFinite)

        // TODO: Don't hardcode here the number of floors
        val level = parts[2].toIntOrNull()?.takeIf { it in (0..1) }

        if (latitude != null && longitude != null && level != null)
            return Location(latitude, longitude, level)
    }

    return null
}