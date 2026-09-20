package com.baden.aitokens.data.repository

import com.baden.aitokens.data.local.CredentialStore
import com.baden.aitokens.data.model.Account
import com.baden.aitokens.data.model.Provider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class AccountRepository(private val store: CredentialStore) {

    private val _accounts = MutableStateFlow(store.load())
    val accounts: StateFlow<List<Account>> = _accounts.asStateFlow()

    fun find(id: String): Account? = _accounts.value.firstOrNull { it.id == id }

    fun add(
        provider: Provider,
        label: String,
        credential: String,
        username: String?,
        region: String?,
        plan: String? = null,
    ) {
        val account = Account(
            id = UUID.randomUUID().toString(),
            provider = provider,
            label = label.trim(),
            credential = credential.trim(),
            username = username?.trim()?.takeIf { it.isNotEmpty() },
            region = region?.takeIf { provider.needsRegion },
            plan = plan?.takeIf { provider.needsPlan },
        )
        update(_accounts.value + account)
    }

    fun remove(id: String) {
        update(_accounts.value.filterNot { it.id == id })
    }

    private fun update(list: List<Account>) {
        _accounts.value = list
        store.save(list)
    }
}
