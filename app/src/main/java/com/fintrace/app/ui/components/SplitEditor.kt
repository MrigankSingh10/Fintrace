package com.fintrace.app.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

data class SplitParticipant(val id: String, val name: String, val shareAmount: String)

@Composable
fun SplitEditor(
    fullBillAmount: Double,
    participants: List<SplitParticipant>,
    computedShareAmount: Double,
    modifier: Modifier = Modifier,
    validationMessage: String? = null,
    onAddParticipant: () -> Unit = {},
    onRemoveParticipant: (String) -> Unit = {},
    onParticipantNameChange: (String, String) -> Unit = { _, _ -> },
    onParticipantShareChange: (String, String) -> Unit = { _, _ -> },
    currencyCode: String = "INR",
    fullBillAmountText: String? = null,
    enabled: Boolean = true,
    expanded: Boolean = true,
    onExpandedChange: (Boolean) -> Unit = {},
    onFullBillAmountChange: (String) -> Unit = {}
) {
    Card(
        modifier = modifier.fillMaxWidth().fintraceCardBorder(RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Split bill", style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    if (!expanded) Text(formatCurrency(fullBillAmount, currencyCode), style = MaterialTheme.typography.labelMedium)
                    TextButton(onClick = { onExpandedChange(!expanded) }, enabled = enabled) {
                        Text(if (expanded) "Collapse" else "Expand")
                    }
                }
            }
            if (expanded) {
                OutlinedTextField(
                    value = fullBillAmountText ?: formatCurrency(fullBillAmount, currencyCode),
                    onValueChange = onFullBillAmountChange,
                    label = { Text("Full bill") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled,
                    singleLine = true
                )
                participants.forEach { participant ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = participant.name,
                                onValueChange = { onParticipantNameChange(participant.id, it) },
                                label = { Text("Participant") },
                                modifier = Modifier.weight(1f),
                                enabled = enabled,
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = participant.shareAmount,
                                onValueChange = { onParticipantShareChange(participant.id, it) },
                                label = { Text("Share") },
                                modifier = Modifier.weight(1f),
                                enabled = enabled,
                                singleLine = true
                            )
                        }
                        TextButton(onClick = { onRemoveParticipant(participant.id) }, enabled = enabled) { Text("Remove ${participant.name}") }
                    }
                }
                TextButton(onClick = onAddParticipant, enabled = enabled) { Text("Add participant") }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Your share", style = MaterialTheme.typography.titleSmall)
                    Text(formatCurrency(computedShareAmount, currencyCode), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                }
                validationMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Preview(name = "Split editor · Light", showBackground = true)
@Composable private fun SplitEditorLightPreview() { FintraceComponentPreview(false) { SplitEditor(2400.0, listOf(SplitParticipant("a", "Me", "1200.00"), SplitParticipant("b", "Alex", "1200.00")), 1200.0) } }
@Preview(name = "Split editor · Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable private fun SplitEditorDarkPreview() { FintraceComponentPreview(true) { SplitEditor(2400.0, listOf(SplitParticipant("a", "Me", "1800.00"), SplitParticipant("b", "Alex", "600.00")), 1800.0, validationMessage = "Shares do not match the full bill") } }
