package com.baden.aitokens.data.model

data class QuotaWindow(
    val title: String,
    val used: Double? = null,
    val total: Double? = null,
    val percentRemaining: Double? = null,
    val resetAtMillis: Long? = null,
    val unlimited: Boolean = false,
    val exactNumbers: Boolean = false,
    val summary: String? = null,
    val note: String? = null,
)

data class Balance(
    val currency: String,
    val toppedUp: Double,
    val granted: Double,
    val total: Double,
    val available: Boolean,
)

data class ProviderUsage(
    val accountId: String,
    val provider: Provider,
    val windows: List<QuotaWindow> = emptyList(),
    val balance: Balance? = null,
    val rawJson: String? = null,
    val fetchedAtMillis: Long = System.currentTimeMillis(),
    val error: String? = null,
) {
    val isSuccess: Boolean get() = error == null
}
