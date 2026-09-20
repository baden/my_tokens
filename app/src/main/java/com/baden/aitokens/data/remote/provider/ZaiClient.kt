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

object ZaiParser {

    @Serializable
    private data class Response(
        val code: Int? = null,
        val msg: String? = null,
        val success: Boolean? = null,
        val data: Data? = null,
    )

    @Serializable
    private data class Data(
        val limits: List<Limit> = emptyList(),
    )

    @Serializable
    private data class Limit(
        val type: String? = null,
        val usage: Double? = null,
        @SerialName("currentValue") val currentValue: Double? = null,
        val percentage: Double? = null,
        @SerialName("nextResetTime") val nextResetTime: Long? = null,
    )

    fun parse(accountId: String, body: String): ProviderUsage {
        val dto = Http.json.decodeFromString<Response>(body)
        if (dto.success == false || (dto.code != null && dto.code != 200)) {
            throw ProviderException("Z.ai: ${dto.msg ?: "помилка відповіді"}", rawBody = body)
        }
        val windows = dto.data?.limits.orEmpty().map { limit ->
            val remaining = limit.percentage?.let { (100.0 - it).coerceIn(0.0, 100.0) }
            QuotaWindow(
                title = when (limit.type) {
                    "TOKENS_LIMIT" -> "5-годинний ліміт токенів"
                    "TIME_LIMIT" -> "MCP (місячний)"
                    else -> limit.type ?: "Ліміт"
                },
                used = limit.currentValue,
                total = limit.usage,
                percentRemaining = remaining,
                resetAtMillis = limit.nextResetTime,
            )
        }
        return ProviderUsage(
            accountId = accountId,
            provider = Provider.ZAI,
            windows = windows,
            rawJson = body,
        )
    }
}

class ZaiClient : UsageProvider {

    override fun fetch(account: Account): ProviderUsage {
        val url = "https://api.z.ai/api/monitor/usage/quota/limit"
        val body = try {
            Http.get(url, headers(account, bearer = false))
        } catch (e: ProviderException) {
            if (e.code == 401 || e.code == 403) {
                Http.get(url, headers(account, bearer = true))
            } else {
                throw e
            }
        }
        return ZaiParser.parse(account.id, body)
    }

    private fun headers(account: Account, bearer: Boolean): Map<String, String> {
        val value = if (bearer) "Bearer ${account.credential}" else account.credential
        return mapOf(
            "Authorization" to value,
            "Accept" to "application/json",
            "Content-Type" to "application/json",
        )
    }
}
