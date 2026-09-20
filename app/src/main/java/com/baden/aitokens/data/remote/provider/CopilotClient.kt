package com.baden.aitokens.data.remote.provider

import com.baden.aitokens.data.model.Account
import com.baden.aitokens.data.model.Provider
import com.baden.aitokens.data.model.ProviderUsage
import com.baden.aitokens.data.model.QuotaWindow
import com.baden.aitokens.data.remote.Http
import com.baden.aitokens.data.remote.ProviderException
import com.baden.aitokens.data.remote.UsageProvider
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import java.net.URLEncoder

object CopilotPlans {

    val ORDER = listOf("free", "pro", "pro+", "business", "enterprise")

    fun limit(plan: String?): Double? = when (plan?.lowercase()) {
        "free" -> 50.0
        "pro" -> 300.0
        "pro+" -> 1500.0
        "business" -> 300.0
        "enterprise" -> 1000.0
        else -> null
    }

    fun display(plan: String): String = when (plan.lowercase()) {
        "free" -> "Free (50)"
        "pro" -> "Pro (300)"
        "pro+" -> "Pro+ (1500)"
        "business" -> "Business (300)"
        "enterprise" -> "Enterprise (1000)"
        else -> plan
    }
}

object CopilotParser {

    @Serializable
    private data class Response(
        val user: String? = null,
        val timePeriod: TimePeriod? = null,
        val usageItems: List<UsageItem> = emptyList(),
    )

    @Serializable
    private data class TimePeriod(
        val year: Int? = null,
        val month: Int? = null,
    )

    @Serializable
    private data class UsageItem(
        val product: String? = null,
        val sku: String? = null,
        val model: String? = null,
        val unitType: String? = null,
        val grossQuantity: Double? = null,
        val netQuantity: Double? = null,
        val limit: Double? = null,
    )

    fun parse(accountId: String, body: String, planLimit: Double? = null): ProviderUsage {
        val dto = Http.json.decodeFromString<Response>(body)
        val items = dto.usageItems
        val used = items.sumOf { it.netQuantity ?: it.grossQuantity ?: 0.0 }
        val total = items.mapNotNull { it.limit }.maxOrNull() ?: planLimit

        val byModel = items
            .groupBy { it.model ?: "—" }
            .mapValues { (_, list) -> list.sumOf { it.netQuantity ?: it.grossQuantity ?: 0.0 } }
            .filterValues { it > 0 }
            .toList()
            .sortedByDescending { it.second }
            .take(6)

        val period = dto.timePeriod?.let { tp ->
            if (tp.year != null && tp.month != null) "%04d-%02d".format(tp.year, tp.month) else null
        }

        val percentRemaining = if (total != null && total > 0) {
            ((total - used) / total * 100).coerceIn(0.0, 100.0)
        } else {
            null
        }

        val noteParts = buildList {
            if (period != null) add("Період: $period")
            if (items.isEmpty()) {
                add("Цього місяця premium requests не використано")
            } else if (byModel.isNotEmpty()) {
                add(byModel.joinToString("\n") { (model, qty) -> "$model: ${formatQty(qty)}" })
            }
            if (total == null) {
                add("Ліміт невідомий — вкажіть план Copilot при додаванні акаунта")
            }
        }

        val window = QuotaWindow(
            title = "Premium requests (місяць)",
            used = used,
            total = total,
            percentRemaining = percentRemaining,
            note = noteParts.takeIf { it.isNotEmpty() }?.joinToString("\n"),
        )

        return ProviderUsage(
            accountId = accountId,
            provider = Provider.COPILOT,
            windows = listOf(window),
            rawJson = body,
        )
    }

    private fun formatQty(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else "%.2f".format(value)
}

class CopilotClient : UsageProvider {

    override fun fetch(account: Account): ProviderUsage {
        val user = account.username?.takeIf { it.isNotBlank() }
            ?: throw ProviderException("Вкажіть GitHub username для Copilot")
        val url = "https://api.github.com/users/${encodePath(user)}/settings/billing/premium_request/usage"
        val body = Http.get(
            url = url,
            headers = mapOf(
                "Accept" to "application/vnd.github+json",
                "Authorization" to "Bearer ${account.credential}",
                "X-GitHub-Api-Version" to "2022-11-28",
                "User-Agent" to "AiTokens-Android",
            ),
        )
        return CopilotParser.parse(account.id, body, CopilotPlans.limit(account.plan))
    }

    private fun encodePath(value: String): String = URLEncoder.encode(value, "UTF-8")
}
