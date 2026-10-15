package com.fintrace.app.ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fintrace.app.data.model.PaymentModeType
import com.fintrace.app.ui.components.formatCurrency
import com.fintrace.app.ui.theme.AccentPurple
import com.fintrace.app.ui.theme.ExpenseRed
import com.fintrace.app.ui.theme.PrimaryBlue
import com.fintrace.app.ui.theme.PrimaryEmerald

private data class CardSplitTotals(
    val myShare: Double,
    val totalShare: Double,
    val splitOwed: Double
)

private fun calculateCardSplitTotals(paymentModes: List<PaymentModeSpendSummary>) = CardSplitTotals(
    myShare = paymentModes.sumOf { it.totalMyShareSpent },
    totalShare = paymentModes.sumOf { it.totalOriginalSpent },
    splitOwed = paymentModes.sumOf { it.totalSplitOwed }
)

@Composable
fun CardSplitSummaryTable(
    paymentModes: List<PaymentModeSpendSummary>,
    modifier: Modifier = Modifier
) {
    val totals = calculateCardSplitTotals(paymentModes)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(PrimaryBlue.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CreditCard,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        "Card & Mode Split Breakdown",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "My Share • Total Share • Split Owed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.size(12.dp))
            if (paymentModes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No card transactions recorded for this period.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val modeWeight = 1.2f
                val amountWeight = 1f
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .42f))
                        .padding(horizontal = 6.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Mode",
                        modifier = Modifier.weight(modeWeight),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "My Share",
                        modifier = Modifier.weight(amountWeight),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End
                    )
                    Text(
                        "Total Share",
                        modifier = Modifier.weight(amountWeight),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End
                    )
                    Text(
                        "Split Owed",
                        modifier = Modifier.weight(amountWeight),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End
                    )
                }
                Spacer(Modifier.size(6.dp))
                paymentModes.forEachIndexed { index, mode ->
                    val isCreditCard = mode.paymentModeType == PaymentModeType.CREDIT_CARD ||
                        mode.paymentModeName.contains("HDFC", ignoreCase = true) ||
                        mode.paymentModeName.contains("ICICI", ignoreCase = true) ||
                        mode.paymentModeName.contains("Card", ignoreCase = true)
                    val iconTint = if (isCreditCard) AccentPurple else PrimaryBlue
                    val rowColor = if (index % 2 == 0) {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .18f)
                    } else {
                        Color.Transparent
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(rowColor)
                            .padding(horizontal = 6.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(modeWeight),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(8.dp).clip(CircleShape).background(iconTint)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                mode.paymentModeName,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            formatCurrency(mode.totalMyShareSpent),
                            modifier = Modifier.weight(amountWeight),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = ExpenseRed,
                            textAlign = TextAlign.End
                        )
                        Text(
                            formatCurrency(mode.totalOriginalSpent),
                            modifier = Modifier.weight(amountWeight),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.End
                        )
                        Text(
                            formatCurrency(mode.totalSplitOwed),
                            modifier = Modifier.weight(amountWeight),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (mode.totalSplitOwed > 0.0) PrimaryEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.End
                        )
                    }
                    if (index != paymentModes.lastIndex) Spacer(Modifier.size(8.dp))
                }

                Spacer(Modifier.size(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .3f))
                Spacer(Modifier.size(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .5f))
                        .padding(horizontal = 6.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Grand Total",
                        modifier = Modifier.weight(modeWeight),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        formatCurrency(totals.myShare),
                        modifier = Modifier.weight(amountWeight),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = ExpenseRed,
                        textAlign = TextAlign.End
                    )
                    Text(
                        formatCurrency(totals.totalShare),
                        modifier = Modifier.weight(amountWeight),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End
                    )
                    Text(
                        formatCurrency(totals.splitOwed),
                        modifier = Modifier.weight(amountWeight),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (totals.splitOwed > 0.0) PrimaryEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}
