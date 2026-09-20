package com.baden.aitokens.data.remote

import com.baden.aitokens.data.model.Account
import com.baden.aitokens.data.model.ProviderUsage

interface UsageProvider {
    fun fetch(account: Account): ProviderUsage
}

fun UsageProvider.safeFetch(account: Account): ProviderUsage = try {
    fetch(account)
} catch (e: Exception) {
    ProviderUsage(
        accountId = account.id,
        provider = account.provider,
        error = e.message ?: "Невідома помилка",
    )
}
