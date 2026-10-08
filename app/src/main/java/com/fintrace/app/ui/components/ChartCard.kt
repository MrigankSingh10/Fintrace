package com.fintrace.app.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun ChartCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth().fintraceCardBorder(RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            content()
        }
    }
}

@Preview(name = "Chart card · Light", showBackground = true)
@Composable private fun ChartCardLightPreview() { FintraceComponentPreview(false) { ChartCard("Spending by category", subtitle = "June 2026") { Text("Chart preview", modifier = Modifier.padding(vertical = 48.dp)) } } }
@Preview(name = "Chart card · Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable private fun ChartCardDarkPreview() { FintraceComponentPreview(true) { ChartCard("Monthly trend", subtitle = "Last 6 months") { Text("Chart preview", modifier = Modifier.padding(vertical = 48.dp)) } } }
