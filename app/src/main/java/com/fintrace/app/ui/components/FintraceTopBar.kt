package com.fintrace.app.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun FintraceTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable () -> Unit = {}
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        navigationIcon?.invoke()
        androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
        actions()
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        if (actionLabel != null && onAction != null) androidx.compose.material3.TextButton(onClick = onAction) { Text(actionLabel) }
    }
}

@Preview(name = "Fintrace top bar · Light", showBackground = true)
@Composable private fun FintraceTopBarLightPreview() { FintraceComponentPreview(false) { FintraceTopBar("Transactions", subtitle = "June 2026", actions = { IconButton(onClick = {}) { Text("⋮") } }) } }
@Preview(name = "Fintrace top bar · Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable private fun FintraceTopBarDarkPreview() { FintraceComponentPreview(true) { FintraceTopBar("A very long section title that should truncate", subtitle = "Dark theme preview") } }
@Preview(name = "Section header · Light", showBackground = true)
@Composable private fun SectionHeaderLightPreview() { FintraceComponentPreview(false) { SectionHeader("Recent activity", actionLabel = "See all", onAction = {}) } }
@Preview(name = "Section header · Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable private fun SectionHeaderDarkPreview() { FintraceComponentPreview(true) { SectionHeader("Pending review") } }
