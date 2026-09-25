package com.fintrace.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fintrace.app.ui.theme.ExpenseRed
import com.fintrace.app.ui.theme.IncomeGreen
import com.fintrace.app.ui.theme.SplitBadgeBg
import com.fintrace.app.ui.theme.SplitBadgeText
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

private val CURRENCY_SYMBOLS = mapOf(
    "INR" to "₹",
    "GBP" to "£",
    "USD" to "$",
    "EUR" to "€",
    "AED" to "AED",
    "SGD" to "S$",
    "CAD" to "CA$",
    "AUD" to "A$",
    "JPY" to "¥"
)

fun currencySymbol(currencyCode: String): String =
    CURRENCY_SYMBOLS[currencyCode]
        ?: runCatching { Currency.getInstance(currencyCode).symbol }.getOrDefault(currencyCode)

fun formatCurrency(amount: Double, currencyCode: String = "INR"): String {
    val formatter = if (currencyCode == "INR") {
        NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    } else {
        NumberFormat.getCurrencyInstance(Locale.getDefault())
    }
    val formatted = runCatching { formatter.format(amount) }.getOrDefault("%.2f".format(amount))
    return when (currencyCode) {
        "INR" -> formatted.replace("INR", "₹").trim()
        else -> {
            val symbol = currencySymbol(currencyCode)
            if (formatted.contains(symbol)) formatted
            else "$symbol ${"%.2f".format(amount)}".trim()
        }
    }
}

@Composable
fun DualAmountDisplay(
    originalAmount: Double,
    myShareAmount: Double,
    isExpense: Boolean = true,
    currencyCode: String = "INR",
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.End
) {
    val isSplit = originalAmount != myShareAmount
    val amountColor = if (isExpense) ExpenseRed else IncomeGreen
    val sign = if (isExpense) "-" else "+"

    Column(
        horizontalAlignment = horizontalAlignment,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSplit) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(SplitBadgeBg)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CallSplit,
                        contentDescription = "Split Expense",
                        tint = SplitBadgeText,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "Split",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = SplitBadgeText
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            Text(
                text = "$sign${formatCurrency(myShareAmount, currencyCode)}",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                ),
                color = amountColor
            )
        }

        if (isSplit) {
            Text(
                text = "Total: ${formatCurrency(originalAmount, currencyCode)}",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
