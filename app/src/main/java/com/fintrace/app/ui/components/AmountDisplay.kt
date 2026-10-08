package com.fintrace.app.ui.components

import android.content.res.Configuration
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fintrace.app.ui.theme.LocalFintraceColors
import com.fintrace.app.ui.theme.MoneyListItem
import com.fintrace.app.ui.theme.SplitBadgeBg
import com.fintrace.app.ui.theme.SplitBadgeText
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import com.fintrace.app.data.model.TransactionType

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

fun currencySymbol(currencyCode: String): String {
    val code = currencyCode.trim().uppercase(Locale.ROOT)
    return CURRENCY_SYMBOLS[code]
        ?: runCatching { Currency.getInstance(code).symbol }.getOrDefault(code)
}

/** Formats money using the requested locale's grouping, decimal, and currency placement rules. */
fun formatMoney(
    amount: Double,
    currencyCode: String = "INR",
    locale: Locale = Locale.getDefault()
): String {
    val code = currencyCode.trim().uppercase(Locale.ROOT).ifEmpty { "INR" }
    val currency = runCatching { Currency.getInstance(code) }.getOrNull()
    val formatter = NumberFormat.getCurrencyInstance(locale)

    if (currency == null) {
        val numberFormatter = NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
            if (locale.country == "IN") isGroupingUsed = false
        }
        val number = numberFormatter.format(amount)
        val localizedNumber = if (locale.country == "IN") {
            groupIndianNumber(number, decimalSeparator(locale), groupingSeparator(locale))
        } else {
            number
        }
        return "$code $localizedNumber"
    }

    formatter.currency = currency
    val fractionDigits = currency.defaultFractionDigits.takeIf { it >= 0 } ?: 2
    formatter.minimumFractionDigits = fractionDigits
    formatter.maximumFractionDigits = fractionDigits
    if (locale.country == "IN") formatter.isGroupingUsed = false
    val formatted = formatter.format(amount)
    if (locale.country != "IN") return formatted
    return groupIndianNumber(formatted, decimalSeparator(locale), groupingSeparator(locale))
}

private fun decimalSeparator(locale: Locale): Char =
    (NumberFormat.getInstance(locale) as? DecimalFormat)?.decimalFormatSymbols?.decimalSeparator ?: '.'

private fun groupingSeparator(locale: Locale): Char =
    (NumberFormat.getInstance(locale) as? DecimalFormat)?.decimalFormatSymbols?.groupingSeparator ?: ','

private fun groupIndianNumber(number: String, decimalSeparator: Char, groupingSeparator: Char): String {
    val decimalIndex = number.indexOf(decimalSeparator).let { if (it < 0) number.length else it }
    val integer = number.substring(0, decimalIndex)
    val firstDigitIndex = integer.indexOfFirst(Char::isDigit)
    if (firstDigitIndex < 0) return number

    val prefix = integer.substring(0, firstDigitIndex)
    val digits = integer.substring(firstDigitIndex)
    if (digits.length <= 3) return number

    val leadingLength = (digits.length - 3) % 2
    val grouped = buildString {
        if (leadingLength > 0) {
            append(digits.substring(0, leadingLength))
            append(groupingSeparator)
        }
        var index = leadingLength
        val pairedEnd = digits.length - 3
        while (index < pairedEnd) {
            append(digits.substring(index, index + 2))
            append(groupingSeparator)
            index += 2
        }
        append(digits.substring(digits.length - 3))
    }
    return prefix + grouped + number.substring(decimalIndex)
}

/** Compatibility wrapper. Existing INR callers retain the app's en-IN grouping behavior. */
fun formatCurrency(amount: Double, currencyCode: String = "INR"): String {
    val locale = if (currencyCode.trim().equals("INR", ignoreCase = true)) {
        Locale("en", "IN")
    } else {
        Locale.getDefault()
    }
    return formatMoney(amount, currencyCode, locale)
}

enum class MoneyTone { Neutral, Income, Expense }

data class SignedTransactionAmount(val amount: Double, val sign: String?)

fun transactionAmountDisplay(type: TransactionType, amount: Double): SignedTransactionAmount = when (type) {
    TransactionType.EXPENSE -> SignedTransactionAmount(kotlin.math.abs(amount), if (amount < 0.0) "+" else "−")
    TransactionType.INCOME -> SignedTransactionAmount(kotlin.math.abs(amount), if (amount < 0.0) "−" else "+")
    TransactionType.TRANSFER -> SignedTransactionAmount(kotlin.math.abs(amount), null)
}

@Composable
fun MoneyText(
    amount: Double,
    modifier: Modifier = Modifier,
    style: TextStyle? = null,
    tone: MoneyTone = MoneyTone.Neutral,
    sign: String? = null,
    currencyCode: String = "INR",
    locale: Locale = Locale.getDefault()
) {
    val semantic = LocalFintraceColors.current
    val color = when (tone) {
        MoneyTone.Neutral -> MaterialTheme.colorScheme.onSurface
        MoneyTone.Income -> semantic.income
        MoneyTone.Expense -> semantic.expense
    }
    Text(
        text = (sign ?: "") + formatMoney(amount, currencyCode, locale),
        modifier = modifier,
        style = (style ?: MoneyListItem).copy(fontFeatureSettings = "tnum"),
        color = color
    )
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
    val semantic = LocalFintraceColors.current
    val amountColor = if (isExpense) semantic.expense else semantic.income
    val sign = if (isExpense) "-" else "+"

    Column(
        horizontalAlignment = horizontalAlignment,
        modifier = modifier
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
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
                style = MoneyListItem,
                color = amountColor
            )
        }

        if (isSplit) {
            Text(
                text = "Total: ${formatCurrency(originalAmount, currencyCode)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MoneyTextPreviewContent() {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Text("Monthly overview", style = MaterialTheme.typography.titleLarge)
        MoneyText(
            amount = 123456.78,
            style = MaterialTheme.typography.displayLarge,
            currencyCode = "INR",
            locale = Locale("en", "IN")
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Groceries", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            MoneyText(amount = 2840.5, tone = MoneyTone.Expense, sign = "−", currencyCode = "INR", locale = Locale("en", "IN"))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Salary", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            MoneyText(amount = 80000.0, tone = MoneyTone.Income, sign = "+", currencyCode = "INR", locale = Locale("en", "IN"))
        }
    }
}

@Preview(name = "Money · Light", showBackground = true)
@Composable
private fun MoneyTextLightPreview() {
    FintraceComponentPreview(darkTheme = false) { MoneyTextPreviewContent() }
}

@Preview(name = "Money · Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun MoneyTextDarkPreview() {
    FintraceComponentPreview(darkTheme = true) { MoneyTextPreviewContent() }
}
