package com.baden.aitokens.data.model

enum class Provider(
    val displayName: String,
    val credentialLabel: String,
    val credentialHint: String,
    val needsUsername: Boolean = false,
    val needsRegion: Boolean = false,
) {
    DEEPSEEK(
        displayName = "DeepSeek",
        credentialLabel = "API key",
        credentialHint = "sk-...",
    ),
    MINIMAX(
        displayName = "MiniMax",
        credentialLabel = "Subscription Key",
        credentialHint = "Token Plan key",
        needsRegion = true,
    ),
    COPILOT(
        displayName = "GitHub Copilot",
        credentialLabel = "Fine-grained PAT",
        credentialHint = "github_pat_...",
        needsUsername = true,
    ),
    ZAI(
        displayName = "Z.ai",
        credentialLabel = "API key",
        credentialHint = "Z.ai API key",
    ),
}
