package com.baden.aitokens.data.remote.provider

import com.baden.aitokens.data.model.Account
import com.baden.aitokens.data.model.Balance
import com.baden.aitokens.data.model.Provider
import com.baden.aitokens.data.model.ProviderUsage
import com.baden.aitokens.data.remote.Http
import com.baden.aitokens.data.remote.UsageProvider
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString

object DeepSeekParser {

    @Serializable
    private data class Response(
        @SerialName("is_available") val isAvailable: Boolean = false,
        @SerialName("balance_infos") val balanceInfos: List<Info> = emptyList(),
    )

    @Serializable
    private data class Info(
        val currency: String = "",
        @SerialName("total_balance") val total: String = "0",
        @SerialName("granted_balance") val granted: String = "0",
        @SerialName("topped_up_balance") val toppedUp: String = "0",
    )

    fun parse(accountId: String, body: String): ProviderUsage {
        val dto = Http.json.decodeFromString<Response>(body)
        val info = dto.balanceInfos.firstOrNull()
        val balance = info?.let {
            Balance(
                currency = it.currency,
                toppedUp = it.toppedUp.toDoubleOrNull() ?: 0.0,
                granted = it.granted.toDoubleOrNull() ?: 0.0,
                total = it.total.toDoubleOrNull() ?: 0.0,
                available = dto.isAvailable,
            )
        }
        return ProviderUsage(
            accountId = accountId,
            provider = Provider.DEEPSEEK,
            balance = balance,
            rawJson = body,
        )
    }
}

class DeepSeekClient : UsageProvider {

    override fun fetch(account: Account): ProviderUsage {
        val body = Http.get(
            url = "https://api.deepseek.com/user/balance",
            headers = mapOf(
                "Authorization" to "Bearer ${account.credential}",
                "Accept" to "application/json",
            ),
        )
        return DeepSeekParser.parse(account.id, body)
    }
}
