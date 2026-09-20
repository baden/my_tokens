package com.baden.aitokens.data.remote.provider

import com.baden.aitokens.data.model.Account
import com.baden.aitokens.data.model.Provider
import com.baden.aitokens.data.model.ProviderUsage
import com.baden.aitokens.data.model.QuotaWindow
import com.baden.aitokens.data.remote.Http
import com.baden.aitokens.data.remote.ProviderException
import com.baden.aitokens.data.remote.UsageProvider
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString

object MiniMaxParser {

    @Serializable
    private data class Response(
        @SerialName("model_remains") val modelRemains: List<ModelRemain> = emptyList(),
    )

    @Serializable
    private data class ModelRemain(
        @SerialName("model_name") val modelName: String? = null,
        @SerialName("current_interval_usage_count") val intervalUsed: Double? = null,
        @SerialName("current_interval_total_count") val intervalTotal: Double? = null,
        @SerialName("current_interval_remaining_percent") val intervalRemainingPercent: Double? = null,
        @SerialName("current_interval_status") val intervalStatus: Int? = null,
        @SerialName("current_weekly_usage_count") val weeklyUsed: Double? = null,
        @SerialName("current_weekly_total_count") val weeklyTotal: Double? = null,
        @SerialName("current_weekly_remaining_percent") val weeklyRemainingPercent: Double? = null,
        @SerialName("end_time") val endTime: Long? = null,
        @SerialName("weekly_end_time") val weeklyEndTime: Long? = null,
    )

    fun parse(accountId: String, body: String): ProviderUsage {
        val dto = Http.json.decodeFromString<Response>(body)
        val main = dto.modelRemains.firstOrNull { it.modelName?.contains("MiniMax-M") == true }
            ?: dto.modelRemains.firstOrNull()

        val windows = buildList {
            if (main != null) {
                val note = main.modelName
                add(
                    window(
                        title = "5-годинне вікно",
                        remainingPercent = main.intervalRemainingPercent,
                        used = main.intervalUsed,
                        total = main.intervalTotal,
                        endTime = main.endTime,
                        note = note,
                    )
                )
                add(
                    window(
                        title = "Тижневе вікно",
                        remainingPercent = main.weeklyRemainingPercent,
                        used = main.weeklyUsed,
                        total = main.weeklyTotal,
                        endTime = main.weeklyEndTime,
                        note = note,
                    )
                )
            }
        }
        return ProviderUsage(
            accountId = accountId,
            provider = Provider.MINIMAX,
            windows = windows,
            rawJson = body,
        )
    }

    private fun window(
        title: String,
        remainingPercent: Double?,
        used: Double?,
        total: Double?,
        endTime: Long?,
        note: String?,
    ): QuotaWindow {
        val remaining = remainingPercent?.coerceIn(0.0, 100.0)
            ?: if (total != null && total > 0 && used != null) {
                ((total - used) / total * 100).coerceIn(0.0, 100.0)
            } else {
                null
            }
        return QuotaWindow(
            title = title,
            used = used,
            total = total,
            percentRemaining = remaining,
            resetAtMillis = toMillis(endTime),
            note = note,
        )
    }

    private fun toMillis(value: Long?): Long? {
        val v = value ?: return null
        if (v <= 0) return null
        return if (v > 1_000_000_000_000L) v else v * 1000
    }
}

class MiniMaxClient : UsageProvider {

    override fun fetch(account: Account): ProviderUsage {
        val base = if (account.region == REGION_CN) {
            "https://api.minimaxi.com"
        } else {
            "https://www.minimax.io"
        }
        val headers = mapOf(
            "Authorization" to "Bearer ${account.credential}",
            "Accept" to "application/json",
        )

        var lastError: ProviderException? = null
        for (path in ENDPOINT_PATHS) {
            try {
                val body = Http.get(base + path, headers)
                return MiniMaxParser.parse(account.id, body)
            } catch (e: ProviderException) {
                lastError = e
                if (e.code != 404 && e.code != 405) throw e
            }
        }
        throw lastError ?: ProviderException("MiniMax: не вдалося отримати дані")
    }

    companion object {
        const val REGION_CN = "cn"
        private val ENDPOINT_PATHS = listOf(
            "/v1/token_plan/remains",
            "/v1/coding_plan/remains",
        )
    }
}
