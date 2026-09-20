package com.baden.aitokens.di

import android.content.Context
import com.baden.aitokens.data.local.CredentialStore
import com.baden.aitokens.data.model.Account
import com.baden.aitokens.data.model.Provider
import com.baden.aitokens.data.model.ProviderUsage
import com.baden.aitokens.data.remote.UsageProvider
import com.baden.aitokens.data.remote.provider.CopilotClient
import com.baden.aitokens.data.remote.provider.DeepSeekClient
import com.baden.aitokens.data.remote.provider.MiniMaxClient
import com.baden.aitokens.data.remote.provider.ZaiClient
import com.baden.aitokens.data.repository.AccountRepository
import com.baden.aitokens.data.repository.UsageRepository

class AppContainer(context: Context) {

    private val credentialStore = CredentialStore(context.applicationContext)
    val accountRepository = AccountRepository(credentialStore)

    private val clients: Map<Provider, UsageProvider> = mapOf(
        Provider.DEEPSEEK to DeepSeekClient(),
        Provider.MINIMAX to MiniMaxClient(),
        Provider.COPILOT to CopilotClient(),
        Provider.ZAI to ZaiClient(),
    )

    private val usageRepository = UsageRepository(clients)

    suspend fun fetchUsage(account: Account): ProviderUsage = usageRepository.fetch(account)
}
