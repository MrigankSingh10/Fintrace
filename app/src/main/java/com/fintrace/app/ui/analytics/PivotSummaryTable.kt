package com.fintrace.app.ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fintrace.app.data.local.relation.CategorySpendSummary
import com.fintrace.app.ui.components.formatCurrency
import com.fintrace.app.ui.components.parseColorHex
import com.fintrace.app.ui.theme.ExpenseRed

private val categoryWidth = 140.dp
private val shareWidth = 110.dp
private val percentWidth = 70.dp
private val originalWidth = 125.dp
private val budgetWidth = 110.dp
private val remainingWidth = 120.dp
private val utilizationWidth = 90.dp
private val tableWidth = categoryWidth + shareWidth + percentWidth + originalWidth + budgetWidth + remainingWidth + utilizationWidth

@Composable
fun PivotSummaryTable(
    categories: List<CategorySpendSummary>,
    totalMyShare: Double,
    totalOriginal: Double,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()) {
                Text("Category Pivot Summary", style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface)
                Text("Excel View", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                TableRow(background = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .6f), header = true) {
                    Cell("Category", categoryWidth, header = true)
                    Cell("My Share", shareWidth, header = true, alignEnd = true)
                    Cell("% Total", percentWidth, header = true, alignEnd = true)
                    Cell("Original Charged", originalWidth, header = true, alignEnd = true)
                    Cell("Budget", budgetWidth, header = true, alignEnd = true)
                    Cell("Remaining", remainingWidth, header = true, alignEnd = true)
                    Cell("% Budget", utilizationWidth, header = true, alignEnd = true)
                }
                if (categories.isEmpty()) {
                    Text("No spends recorded in this period.", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(tableWidth).padding(vertical = 24.dp))
                } else {
                    categories.forEachIndexed { index, cat ->
                        val ratio = if (totalMyShare > 0.0) cat.totalMyShareSpent / totalMyShare * 100.0 else 0.0
                        val rowColor = if (index % 2 == 0) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .2f)
                        TableRow(background = rowColor) {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.width(categoryWidth).padding(horizontal = 8.dp, vertical = 9.dp)) {
                                Box(Modifier.size(8.dp).clip(CircleShape).background(parseColorHex(cat.colorHex)))
                                Spacer(Modifier.width(6.dp))
                                Text(cat.categoryName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Cell(formatCurrency(cat.totalMyShareSpent), shareWidth, alignEnd = true, emphasized = true)
                            Cell("${String.format("%.1f", ratio)}%", percentWidth, alignEnd = true, secondary = true)
                            Cell(formatCurrency(cat.totalOriginalSpent), originalWidth, alignEnd = true, secondary = true)
                            Cell(cat.budgetAmount?.let(::formatCurrency) ?: "-", budgetWidth, alignEnd = true, secondary = true)
                            Cell(cat.budgetRemaining?.let(::formatCurrency) ?: "-", remainingWidth, alignEnd = true,
                                color = if ((cat.budgetRemaining ?: 0.0) < 0.0) ExpenseRed else MaterialTheme.colorScheme.onSurface)
                            Cell(cat.budgetUtilization?.let { String.format("%.1f%%", it * 100.0) } ?: "-", utilizationWidth,
                                alignEnd = true, color = if ((cat.budgetUtilization ?: 0.0) > 1.0) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.size(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .3f))
                    Spacer(Modifier.size(8.dp))
                    val totalBudget = categories.mapNotNull { it.budgetAmount }.sum()
                    val totalRemaining = totalBudget - totalMyShare
                    TableRow(background = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .5f), total = true) {
                        Cell("Grand Total", categoryWidth, total = true)
                        Cell(formatCurrency(totalMyShare), shareWidth, alignEnd = true, total = true, color = ExpenseRed)
                        Cell(if (totalMyShare > 0.0) "100.0%" else "0.0%", percentWidth, alignEnd = true, total = true)
                        Cell(formatCurrency(totalOriginal), originalWidth, alignEnd = true, total = true, secondary = true)
                        Cell(if (totalBudget > 0) formatCurrency(totalBudget) else "-", budgetWidth, alignEnd = true, total = true, secondary = true)
                        Cell(if (totalBudget > 0) formatCurrency(totalRemaining) else "-", remainingWidth, alignEnd = true, total = true,
                            color = if (totalRemaining < 0.0) ExpenseRed else MaterialTheme.colorScheme.onSurface)
                        Cell(if (totalBudget > 0) String.format("%.1f%%", totalMyShare / totalBudget * 100.0) else "-",
                            utilizationWidth, alignEnd = true, total = true,
                            color = if (totalBudget > 0 && totalMyShare > totalBudget) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun TableRow(background: Color, header: Boolean = false, total: Boolean = false, content: @Composable RowScope.() -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.width(tableWidth).clip(RoundedCornerShape(if (header || total) 8.dp else 6.dp)).background(background),
        content = content)
}

@Composable
private fun Cell(
    value: String,
    width: androidx.compose.ui.unit.Dp,
    header: Boolean = false,
    alignEnd: Boolean = false,
    total: Boolean = false,
    emphasized: Boolean = false,
    secondary: Boolean = false,
    color: Color = if (secondary) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
) {
    Box(modifier = Modifier.width(width).padding(horizontal = 8.dp, vertical = if (header || total) 10.dp else 9.dp),
        contentAlignment = if (alignEnd) Alignment.CenterEnd else Alignment.CenterStart) {
        Text(value,
            style = when {
                header -> MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                total -> MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                emphasized -> MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                else -> MaterialTheme.typography.bodySmall
            },
            color = color, textAlign = if (alignEnd) TextAlign.End else TextAlign.Start,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
