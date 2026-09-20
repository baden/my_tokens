package com.baden.aitokens.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Account(
    val id: String,
    val provider: Provider,
    val label: String = "",
    val credential: String,
    val username: String? = null,
    val region: String? = null,
    val plan: String? = null,
) {
    val title: String get() = label.ifBlank { provider.displayName }
}
