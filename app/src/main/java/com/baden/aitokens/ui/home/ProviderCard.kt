package com.baden.aitokens.ui.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.baden.aitokens.data.model.Account
import com.baden.aitokens.data.model.Balance
import com.baden.aitokens.data.model.ProviderUsage
import com.baden.aitokens.data.model.QuotaWindow
import com.baden.aitokens.ui.util.Format

@Composable
fun ProviderCard(account: Account, usage: ProviderUsage?) {
    var expanded by rememberSaveable(account.id) { mutableStateOf(false) }
    var rawExpanded by rememberSaveable(account.id) { mutableStateOf(false) }
    val raw = usage?.rawJson?.takeIf { it.isNotBlank() }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = account.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (account.label.isNotBlank()) {
                        Text(
                            text = account.provider.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
                if (usage != null) {
                    Text(
                        text = Format.updatedAgo(usage.fetchedAtMillis),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (raw != null) {
                    IconButton(onClick = { rawExpanded = !rawExpanded }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Filled.BugReport,
                            contentDescription = "Відповідь API",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Згорнути" else "Розгорнути",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            when {
                usage == null -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                usage.error != null -> {
                    Text(
                        text = "Помилка: ${usage.error}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                else -> {
                    usage.balance?.let { BalanceContent(it) }
                    usage.windows.forEach { QuotaWindowContent(it, expanded) }
                    if (usage.balance == null && usage.windows.isEmpty()) {
                        Text("Немає даних", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            if (rawExpanded && raw != null) {
                val note = usage?.windows
                    ?.mapNotNull { it.note?.takeIf { text -> text.isNotBlank() } }
                    ?.distinct()
                    ?.joinToString("\n\n")
                    ?.takeIf { it.isNotBlank() }
                RawPanel(note = note, raw = raw)
            }
        }
    }
}

@Composable
private fun BalanceContent(balance: Balance) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "${Format.amount(balance.toppedUp)} ${balance.currency}",
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.size(6.dp))
            Text(
                text = "topped-up",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = "Загалом: ${Format.amount(balance.total)} • Бонус: ${Format.amount(balance.granted)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!balance.available) {
            Text(
                text = "Баланс недостатній для запитів",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun QuotaWindowContent(window: QuotaWindow, expanded: Boolean) {
    val fraction = when {
        window.percentRemaining != null -> ((100.0 - window.percentRemaining) / 100.0).coerceIn(0.0, 1.0)
        window.total != null && window.total > 0 && window.used != null -> (window.used / window.total).coerceIn(0.0, 1.0)
        else -> null
    }
    val usedPercent = when {
        window.percentRemaining != null -> 100.0 - window.percentRemaining
        window.total != null && window.total > 0 && window.used != null -> window.used / window.total * 100.0
        else -> null
    }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = window.title,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = when {
                    window.unlimited -> "безліміт"
                    usedPercent != null -> "${usedPercent.toInt()}%"
                    window.used != null -> Format.amount(window.used)
                    else -> ""
                },
                style = MaterialTheme.typography.labelMedium,
            )
        }
        if (fraction != null) {
            LinearProgressIndicator(
                progress = { fraction.toFloat() },
                modifier = Modifier.fillMaxWidth().height(4.dp),
            )
        }
        window.summary?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (expanded) {
            val fmt: (Double) -> String = if (window.exactNumbers) Format::exact else Format::amount
            val usedTotal = if (window.used != null && window.total != null) {
                "${fmt(window.used)} / ${fmt(window.total)}"
            } else {
                null
            }
            val reset = Format.resetShort(window.resetAtMillis)?.let { "↻ $it" }
            val meta = listOfNotNull(usedTotal, reset).joinToString(" • ")
            if (meta.isNotEmpty()) {
                Text(
                    text = meta,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RawPanel(note: String?, raw: String) {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Відповідь API",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("API response", raw))
                    Toast.makeText(context, "Скопійовано", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = "Копіювати",
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        note?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SelectionContainer {
            Text(
                text = raw,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}
