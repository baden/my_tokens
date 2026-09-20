package com.baden.aitokens.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.baden.aitokens.data.model.Account
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CredentialStore(context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val prefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun load(): List<Account> {
        val raw = prefs.getString(KEY_ACCOUNTS, null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<Account>>(raw) }.getOrDefault(emptyList())
    }

    fun save(accounts: List<Account>) {
        prefs.edit().putString(KEY_ACCOUNTS, json.encodeToString(accounts)).apply()
    }

    private companion object {
        const val FILE_NAME = "ai_tokens_secure"
        const val KEY_ACCOUNTS = "accounts"
    }
}
