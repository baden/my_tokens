package com.baden.aitokens.ui.util

import java.util.Locale

object Format {

    fun amount(value: Double): String {
        val abs = kotlin.math.abs(value)
        return when {
            abs >= 1_000_000_000 -> String.format(Locale.US, "%.2fB", value / 1e9)
            abs >= 1_000_000 -> String.format(Locale.US, "%.2fM", value / 1e6)
            abs >= 1_000 -> String.format(Locale.US, "%.1fK", value / 1e3)
            value % 1.0 == 0.0 -> value.toLong().toString()
            else -> String.format(Locale.US, "%.2f", value)
        }
    }

    fun exact(value: Double): String =
        if (value % 1.0 == 0.0) {
            String.format(Locale.US, "%,d", value.toLong())
        } else {
            String.format(Locale.US, "%,.2f", value)
        }

    fun percentRemaining(value: Double?): String? =
        value?.let { "${it.toInt()}% залишилось" }

    fun reset(resetAtMillis: Long?): String? {
        if (resetAtMillis == null) return null
        val diff = resetAtMillis - System.currentTimeMillis()
        if (diff <= 0) return "скидання ось-ось"
        val hours = diff / 3_600_000
        val minutes = (diff % 3_600_000) / 60_000
        return when {
            hours > 0 -> "скидання через ${hours}г ${minutes}хв"
            minutes > 0 -> "скидання через ${minutes}хв"
            else -> "скидання через <1хв"
        }
    }

    fun updatedAgo(fetchedAtMillis: Long): String {
        val minutes = (System.currentTimeMillis() - fetchedAtMillis) / 60_000
        return when {
            minutes < 1 -> "оновлено щойно"
            minutes < 60 -> "оновлено $minutes хв тому"
            else -> "оновлено ${minutes / 60} год тому"
        }
    }
}
