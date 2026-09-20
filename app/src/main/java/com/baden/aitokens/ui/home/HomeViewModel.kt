package com.baden.aitokens.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.baden.aitokens.App
import com.baden.aitokens.data.model.Account
import com.baden.aitokens.data.model.ProviderUsage
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as App).container
    private val accountRepository = container.accountRepository

    val accounts: StateFlow<List<Account>> = accountRepository.accounts

    private val _usages = MutableStateFlow<Map<String, ProviderUsage>>(emptyMap())
    val usages: StateFlow<Map<String, ProviderUsage>> = _usages.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private var refreshJob: Job? = null

    init {
        viewModelScope.launch {
            accountRepository.accounts.collect { list ->
                val ids = list.map { it.id }.toSet()
                _usages.update { current -> current.filterKeys { it in ids } }
                refreshAll()
            }
        }
    }

    fun refreshAll() {
        val list = accounts.value
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _refreshing.value = true
            try {
                list.forEach { account ->
                    launch {
                        val usage = container.fetchUsage(account)
                        _usages.update { it + (account.id to usage) }
                    }
                }
            } finally {
                _refreshing.value = false
            }
        }
    }
}
