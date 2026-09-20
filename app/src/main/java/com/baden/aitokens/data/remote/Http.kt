package com.baden.aitokens.data.remote

import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

object Http {

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .build()

    fun get(url: String, headers: Map<String, String>): String {
        val request = Request.Builder()
            .url(url)
            .apply { headers.forEach { (name, value) -> header(name, value) } }
            .get()
            .build()
        return execute(request)
    }

    private fun execute(request: Request): String {
        val response = try {
            client.newCall(request).execute()
        } catch (e: IOException) {
            throw ProviderException("Немає з'єднання: ${e.message ?: "мережа недоступна"}")
        }
        response.use { res ->
            val body = res.body?.string().orEmpty()
            if (!res.isSuccessful) {
                throw ProviderException(describeError(res.code, body), res.code, body)
            }
            return body
        }
    }

    private fun describeError(code: Int, body: String): String {
        val short = body.take(300).replace("\n", " ").trim()
        return when (code) {
            401 -> "Невірний або протермінований ключ (401)"
            403 -> "Доступ заборонено (403). Перевірте права ключа"
            404 -> "Endpoint не знайдено (404)"
            429 -> "Перевищено ліміт запитів (429)"
            else -> "Помилка HTTP $code" + if (short.isNotEmpty()) ": $short" else ""
        }
    }
}
