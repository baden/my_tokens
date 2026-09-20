package com.baden.aitokens.data.repository

import com.baden.aitokens.data.model.Account
import com.baden.aitokens.data.model.Provider
import com.baden.aitokens.data.model.ProviderUsage
import com.baden.aitokens.data.remote.UsageProvider
import com.baden.aitokens.data.remote.safeFetch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UsageRepository(private val clients: Map<Provider, UsageProvider>) {

    suspend fun fetch(account: Account): ProviderUsage = withContext(Dispatchers.IO) {
        val client = clients[account.provider]
            ?: return@withContext ProviderUsage(
                accountId = account.id,
                provider = account.provider,
                error = "Провайдер не підтримується",
            )
        client.safeFetch(account)
    }
}
