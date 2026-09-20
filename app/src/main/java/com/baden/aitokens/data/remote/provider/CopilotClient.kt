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
import java.util.Locale

object CopilotPlans {

    val ORDER = listOf("free", "pro", "pro+", "max", "business", "enterprise")

    fun limit(plan: String?): Double? = when (plan?.lowercase()) {
        "pro" -> 1_500.0
        "pro+" -> 7_000.0
        "max" -> 20_000.0
        "business" -> 1_900.0
        "enterprise" -> 3_900.0
        else -> null
    }

    fun display(plan: String): String = when (plan.lowercase()) {
        "free" -> "Free"
        "pro" -> "Pro (1500)"
        "pro+" -> "Pro+ (7000)"
        "max" -> "Max (20000)"
        "business" -> "Business (1900)"
        "enterprise" -> "Enterprise (3900)"
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
        val pricePerUnit: Double? = null,
        val grossQuantity: Double? = null,
        val grossAmount: Double? = null,
        val discountQuantity: Double? = null,
        val discountAmount: Double? = null,
        val netQuantity: Double? = null,
        val netAmount: Double? = null,
        val limit: Double? = null,
    )

    fun parse(accountId: String, body: String, planLimit: Double? = null): ProviderUsage {
        val dto = Http.json.decodeFromString<Response>(body)
        val items = dto.usageItems
        val used = items.sumOf { it.netQuantity ?: it.grossQuantity ?: 0.0 }
        val amount = items.sumOf { it.netAmount ?: it.grossAmount ?: 0.0 }
        val total = items.mapNotNull { it.limit }.maxOrNull() ?: planLimit

        val byModel = items
            .groupBy { it.model ?: "—" }
            .mapValues { (_, list) -> list.sumOf { it.netQuantity ?: it.grossQuantity ?: 0.0 } }
            .filterValues { it > 0 }
            .toList()
            .sortedByDescending { it.second }
            .take(8)

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
                add("Цього місяця AI credits не витрачено")
            } else {
                if (amount > 0) add("Витрачено ≈ " + String.format(Locale.US, "$%.2f", amount))
                if (byModel.isNotEmpty()) {
                    add(byModel.joinToString("\n") { (model, qty) -> "$model: ${formatQty(qty)}" })
                }
            }
            if (total == null) {
                add("Ліміт невідомий — вкажіть план Copilot при додаванні акаунта")
            }
        }

        val window = QuotaWindow(
            title = "AI credits (місяць)",
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
        if (value % 1.0 == 0.0) value.toLong().toString() else String.format(Locale.US, "%.2f", value)
}

class CopilotClient : UsageProvider {

    override fun fetch(account: Account): ProviderUsage {
        val user = account.username?.takeIf { it.isNotBlank() }
            ?: throw ProviderException("Вкажіть GitHub username для Copilot")
        val base = "https://api.github.com/users/${encodePath(user)}/settings/billing"
        val headers = mapOf(
            "Accept" to "application/vnd.github+json",
            "Authorization" to "Bearer ${account.credential}",
            "X-GitHub-Api-Version" to "2022-11-28",
            "User-Agent" to "AiTokens-Android",
        )
        val planLimit = CopilotPlans.limit(account.plan)

        val aiUsage = CopilotParser.parse(account.id, Http.get("$base/ai_credit/usage", headers), planLimit)
        if (hasUsage(aiUsage)) return aiUsage

        val legacyUsage = CopilotParser.parse(account.id, Http.get("$base/premium_request/usage", headers), planLimit)
        return if (hasUsage(legacyUsage)) legacyUsage else aiUsage
    }

    private fun hasUsage(usage: ProviderUsage): Boolean =
        (usage.windows.firstOrNull()?.used ?: 0.0) > 0.0

    private fun encodePath(value: String): String = URLEncoder.encode(value, "UTF-8")
}
