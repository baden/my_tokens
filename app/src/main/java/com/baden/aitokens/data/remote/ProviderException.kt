package com.baden.aitokens.data.remote

class ProviderException(
    message: String,
    val code: Int? = null,
    val rawBody: String? = null,
) : Exception(message)
