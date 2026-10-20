package com.example.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object IstTimeFormatter {
    private val istZone = ZoneId.of("Asia/Kolkata")
    private val shortFormatter = DateTimeFormatter.ofPattern("d MMM, h:mm a", Locale.ENGLISH)
    private val fullFormatter = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a 'IST'", Locale.ENGLISH)

    fun formatIstDateShort(unixTimestamp: Long): String {
        return try {
            val instant = Instant.ofEpochSecond(unixTimestamp)
            instant.atZone(istZone).format(shortFormatter)
        } catch (e: Exception) {
            "TBA"
        }
    }

    fun formatIstDateTime(unixTimestamp: Long): String {
        return try {
            val instant = Instant.ofEpochSecond(unixTimestamp)
            instant.atZone(istZone).format(fullFormatter)
        } catch (e: Exception) {
            "TBA"
        }
    }
}
