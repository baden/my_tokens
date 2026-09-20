package com.baden.aitokens.ui.accounts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.baden.aitokens.App
import com.baden.aitokens.data.model.Account
import com.baden.aitokens.data.model.Provider
import kotlinx.coroutines.flow.StateFlow

class AccountsViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = (app as App).container.accountRepository

    val accounts: StateFlow<List<Account>> = repository.accounts

    fun add(
        provider: Provider,
        label: String,
        credential: String,
        username: String?,
        region: String?,
    ) {
        repository.add(provider, label, credential, username, region)
    }

    fun remove(id: String) {
        repository.remove(id)
    }
}
