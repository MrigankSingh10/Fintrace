package com.fintrace.app.ui.navigation

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

private const val PRIVACY_POLICY_URL =
    "https://github.com/MrigankSingh10/Fintrace/blob/main/PRIVACY.md"

@Composable
fun SettingsScreen(
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onNavigateBack: () -> Unit,
    onCategories: () -> Unit,
    onPaymentModes: () -> Unit,
    onExport: () -> Unit,
    onSmsReview: () -> Unit
) {
    var privacyPolicyUnavailable by remember { mutableStateOf(false) }
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Settings", style = MaterialTheme.typography.titleLarge)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(8.dp))
            SettingsRow(
                icon = Icons.Default.Category,
                title = "Categories",
                subtitle = "Manage spending categories",
                onClick = onCategories
            )
            SettingsRow(
                icon = Icons.Default.CreditCard,
                title = "Payment modes",
                subtitle = "Manage cards, accounts, and payment methods",
                onClick = onPaymentModes
            )
            SettingsRow(
                icon = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                title = "Dark theme",
                subtitle = if (isDarkTheme) "On" else "Off",
                trailing = { Switch(checked = isDarkTheme, onCheckedChange = { onThemeToggle() }) },
                onClick = null
            )
            SettingsRow(
                icon = Icons.Default.FileDownload,
                title = "Export report",
                subtitle = "Save an Excel report",
                onClick = onExport
            )
            SettingsRow(
                icon = Icons.Default.MarkEmailUnread,
                title = "SMS Review",
                subtitle = "Review transactions detected from messages",
                onClick = onSmsReview
            )
            SettingsRow(
                icon = Icons.Default.PrivacyTip,
                title = "Privacy policy",
                subtitle = "How Fintrace handles your information",
                onClick = {
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL)))
                    } catch (_: ActivityNotFoundException) {
                        privacyPolicyUnavailable = true
                    } catch (_: SecurityException) {
                        privacyPolicyUnavailable = true
                    }
                }
            )
        }
    }

    if (privacyPolicyUnavailable) {
        AlertDialog(
            onDismissRequest = { privacyPolicyUnavailable = false },
            title = { Text("Can't open privacy policy") },
            text = { Text("No browser or compatible app is available to open the policy link. You can read PRIVACY.md in the Fintrace GitHub repository.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { privacyPolicyUnavailable = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)?,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick, role = Role.Button) else Modifier)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing?.invoke()
    }
}
