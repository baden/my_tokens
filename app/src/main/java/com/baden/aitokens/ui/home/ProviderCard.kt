package com.baden.aitokens.ui.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.baden.aitokens.data.model.Account
import com.baden.aitokens.data.model.Balance
import com.baden.aitokens.data.model.ProviderUsage
import com.baden.aitokens.data.model.QuotaWindow
import com.baden.aitokens.ui.util.Format

@Composable
fun ProviderCard(account: Account, usage: ProviderUsage?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(account.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = account.provider.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (usage != null) {
                    Text(
                        text = Format.updatedAgo(usage.fetchedAtMillis),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            when {
                usage == null -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text("Завантаження…", style = MaterialTheme.typography.bodySmall)
                }

                usage.error != null -> {
                    Text(
                        text = "Помилка: ${usage.error}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                else -> {
                    usage.balance?.let { BalanceContent(it) }
                    usage.windows.forEach { QuotaWindowContent(it) }
                    if (usage.balance == null && usage.windows.isEmpty()) {
                        Text("Немає даних", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            usage?.rawJson?.takeIf { it.isNotBlank() }?.let { raw ->
                RawJsonSection(raw)
            }
        }
    }
}

@Composable
private fun BalanceContent(balance: Balance) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = "${Format.amount(balance.toppedUp)} ${balance.currency}",
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = "Поповнено (topped-up)",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Всього: ${Format.amount(balance.total)} • Бонус: ${Format.amount(balance.granted)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!balance.available) {
            Text(
                text = "Баланс недостатній для запитів",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun QuotaWindowContent(window: QuotaWindow) {
    val fraction = when {
        window.percentRemaining != null -> ((100.0 - window.percentRemaining) / 100.0).coerceIn(0.0, 1.0)
        window.total != null && window.total > 0 && window.used != null -> (window.used / window.total).coerceIn(0.0, 1.0)
        else -> null
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(window.title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(
                text = when {
                    window.unlimited -> "безліміт"
                    window.percentRemaining != null -> "${window.percentRemaining.toInt()}% залишилось"
                    window.used != null -> "використано ${Format.amount(window.used)}"
                    else -> ""
                },
                style = MaterialTheme.typography.labelLarge,
            )
        }
        if (fraction != null) {
            LinearProgressIndicator(
                progress = { fraction.toFloat() },
                modifier = Modifier.fillMaxWidth().height(6.dp),
            )
        }
        val usedTotal = if (window.used != null && window.total != null) {
            "${Format.amount(window.used)} / ${Format.amount(window.total)}"
        } else {
            null
        }
        val reset = Format.reset(window.resetAtMillis)
        if (usedTotal != null || reset != null) {
            Text(
                text = listOfNotNull(usedTotal, reset).joinToString(" • "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        window.note?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RawJsonSection(raw: String) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) "Сховати відповідь API" else "Показати відповідь API")
            }
            if (expanded) {
                TextButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("API response", raw))
                        Toast.makeText(context, "Скопійовано", Toast.LENGTH_SHORT).show()
                    },
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Копіювати")
                }
            }
        }
        AnimatedVisibility(visible = expanded) {
            SelectionContainer {
                Text(
                    text = raw,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}
