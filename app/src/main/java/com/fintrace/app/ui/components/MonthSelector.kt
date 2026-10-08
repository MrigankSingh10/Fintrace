package com.fintrace.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import android.content.res.Configuration
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun MonthSelector(
    month: YearMonth,
    onMonthSelected: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
    showThisMonth: Boolean = true
) {
    var dialogOpen by remember { mutableStateOf(false) }
    var dialogYear by remember(month) { mutableStateOf(month.year) }
    val label = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            IconButton(
                onClick = { onMonthSelected(month.minusMonths(1)) },
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    .semantics { contentDescription = "Previous month" }
            ) { Icon(Icons.Default.ArrowBackIosNew, contentDescription = null) }

            TextButton(
                onClick = { dialogYear = month.year; dialogOpen = true },
                modifier = Modifier.weight(1f).sizeIn(minHeight = 48.dp)
                    .semantics { contentDescription = "Choose month, $label" }
            ) {
                Text(label, textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }

            IconButton(
                onClick = { onMonthSelected(month.plusMonths(1)) },
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    .semantics { contentDescription = "Next month" }
            ) { Icon(Icons.Default.ArrowForwardIos, contentDescription = null) }
        }
        if (showThisMonth) {
            TextButton(
                onClick = { onMonthSelected(YearMonth.now()) },
                modifier = Modifier.align(Alignment.CenterHorizontally).sizeIn(minHeight = 48.dp)
            ) { Text("This month") }
        }
    }

    if (dialogOpen) {
        AlertDialog(
            onDismissRequest = { dialogOpen = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()) {
                    IconButton(onClick = { dialogYear-- }, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .semantics { contentDescription = "Previous year" }) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = null)
                    }
                    Text(dialogYear.toString(), style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = { dialogYear++ }, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .semantics { contentDescription = "Next year" }) {
                        Icon(Icons.Default.ArrowForwardIos, contentDescription = null)
                    }
                }
            },
            text = {
                Column(modifier = Modifier.sizeIn(maxHeight = 360.dp)) {
                    (0..3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                            (0..2).forEach { column ->
                                val monthIndex = row * 3 + column + 1
                                val choice = YearMonth.of(dialogYear, monthIndex)
                                TextButton(
                                    onClick = { onMonthSelected(choice); dialogOpen = false },
                                    modifier = Modifier.weight(1f).sizeIn(minHeight = 48.dp)
                                        .semantics { contentDescription = choice.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())) }
                                ) {
                                    Text(choice.format(DateTimeFormatter.ofPattern("MMM", Locale.getDefault())), textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { dialogOpen = false }) { Text("Close") } }
        )
    }
}

/** Month navigation for new screens; the current month is the latest selectable month. */
@Composable
fun MonthSwitcher(
    month: YearMonth,
    onMonthSelected: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
    currentMonth: YearMonth = YearMonth.now()
) {
    var dialogOpen by remember { mutableStateOf(false) }
    var dialogYear by remember(month) { mutableStateOf(month.year) }
    val label = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth()
    ) {
        IconButton(
            onClick = { onMonthSelected(month.minusMonths(1)) },
            modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .semantics { contentDescription = "Previous month" }
        ) { Icon(Icons.Default.ArrowBackIosNew, contentDescription = null) }
        TextButton(
            onClick = { dialogYear = month.year; dialogOpen = true },
            modifier = Modifier.weight(1f).sizeIn(minHeight = 48.dp)
                .semantics { contentDescription = "Choose month, $label" }
        ) {
            Text(label, textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        }
        IconButton(
            onClick = { if (month < currentMonth) onMonthSelected(month.plusMonths(1)) },
            enabled = month < currentMonth,
            modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .semantics { contentDescription = "Next month" }
        ) { Icon(Icons.Default.ArrowForwardIos, contentDescription = null) }
    }

    if (dialogOpen) {
        AlertDialog(
            onDismissRequest = { dialogOpen = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()) {
                    IconButton(onClick = { dialogYear-- }, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .semantics { contentDescription = "Previous year" }) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = null)
                    }
                    Text(dialogYear.toString(), style = MaterialTheme.typography.titleLarge)
                    IconButton(
                        onClick = { if (YearMonth.of(dialogYear + 1, 1) <= currentMonth) dialogYear++ },
                        enabled = YearMonth.of(dialogYear + 1, 1) <= currentMonth,
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            .semantics { contentDescription = "Next year" }
                    ) { Icon(Icons.Default.ArrowForwardIos, contentDescription = null) }
                }
            },
            text = {
                Column(modifier = Modifier.sizeIn(maxHeight = 360.dp)) {
                    (0..3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                            (0..2).forEach { column ->
                                val choice = YearMonth.of(dialogYear, row * 3 + column + 1)
                                val selectable = choice <= currentMonth
                                TextButton(
                                    onClick = { if (selectable) { onMonthSelected(choice); dialogOpen = false } },
                                    enabled = selectable,
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.onSurface,
                                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                    ),
                                    modifier = Modifier.weight(1f).sizeIn(minHeight = 48.dp)
                                        .semantics { contentDescription = choice.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())) }
                                ) {
                                    Text(choice.format(DateTimeFormatter.ofPattern("MMM", Locale.getDefault())), textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { dialogOpen = false }) { Text("Close") } }
        )
    }
}

@Preview(name = "Month switcher · Light", showBackground = true)
@Composable private fun MonthSwitcherLightPreview() { FintraceComponentPreview(false) { MonthSwitcher(YearMonth.of(2026, 6), {}, currentMonth = YearMonth.of(2026, 6)) } }

@Preview(name = "Month switcher · Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable private fun MonthSwitcherDarkPreview() { FintraceComponentPreview(true) { MonthSwitcher(YearMonth.of(2026, 5), {}, currentMonth = YearMonth.of(2026, 6)) } }
