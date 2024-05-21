/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.bric3.fireplace.charts

import java.awt.Color
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit.MILLISECONDS


fun Color.withAlpha(alpha: Float) = withAlpha((alpha * 255).toInt())

fun Color.withAlpha(alpha: Int): Color {
    require(alpha in 0..255) { "Alpha value must be between 0 and 255" }
    return Color(
        red,
        green,
        blue,
        alpha
    )
}

fun presentableDuration(durationMs: Long): String {
    val millis = durationMs % 1000L
    val seconds = MILLISECONDS.toSeconds(durationMs) % 60L
    val minutes = MILLISECONDS.toMinutes(durationMs) % 60L
    val hours = MILLISECONDS.toHours(durationMs)
    return String.format(
        "%02d:%02d:%02d.%03d",
        hours, minutes, seconds, millis
    )
}

private val timestampFormatter = DateTimeFormatter.ofPattern("LLL d, H:mm:ss")
fun presentableTime(epochMs: Long): String {
    return timestampFormatter.format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()))
}

fun presentablePercentage(value: Double): String {
    return String.format("%.2f%%", value * 100)
}