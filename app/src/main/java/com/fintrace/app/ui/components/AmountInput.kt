package com.fintrace.app.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun AmountInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Amount",
    error: String? = null,
    enabled: Boolean = true,
    currencyPrefix: String = "₹"
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        label = { Text(label) },
        prefix = { Text(currencyPrefix, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        singleLine = true,
        textStyle = MaterialTheme.typography.displaySmall.copy(
            fontSize = 40.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End
        ),
        visualTransformation = IndianAmountVisualTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    )
}

/** Displays ungrouped amount input with Indian digit grouping while keeping the raw value unchanged. */
internal object IndianAmountVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        if (!isSupportedAmount(raw)) return TransformedText(text, OffsetMapping.Identity)

        val signLength = if (raw.startsWith('+') || raw.startsWith('-')) 1 else 0
        val decimalIndex = raw.indexOf('.').let { if (it < 0) raw.length else it }
        val digitCount = decimalIndex - signLength
        if (digitCount <= 3) return TransformedText(text, OffsetMapping.Identity)

        val commaPositions = mutableListOf<Int>()
        var position = digitCount - 3
        while (position > 0) {
            commaPositions += signLength + position
            position -= 2
        }

        val transformed = buildString(raw.length + commaPositions.size) {
            raw.forEachIndexed { index, char ->
                if (index in commaPositions) append(',')
                append(char)
            }
        }
        val originalToTransformed = IntArray(raw.length + 1) { offset ->
            offset + commaPositions.count { it <= offset }
        }
        val transformedToOriginal = IntArray(transformed.length + 1)
        var originalOffset = 0
        transformed.forEachIndexed { index, char ->
            transformedToOriginal[index] = originalOffset
            if (char != ',') originalOffset++
        }
        transformedToOriginal[transformed.length] = originalOffset

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                originalToTransformed[offset.coerceIn(0, raw.length)]

            override fun transformedToOriginal(offset: Int): Int =
                transformedToOriginal[offset.coerceIn(0, transformed.length)]
        }
        return TransformedText(AnnotatedString(transformed), offsetMapping)
    }

    private fun isSupportedAmount(value: String): Boolean {
        val start = if (value.startsWith('+') || value.startsWith('-')) 1 else 0
        var decimalSeen = false
        for (index in start until value.length) {
            val char = value[index]
            when {
                char.isDigit() -> Unit
                char == '.' && !decimalSeen -> decimalSeen = true
                else -> return false
            }
        }
        return true
    }
}

@Preview(name = "Amount input · Light", showBackground = true)
@Composable private fun AmountInputLightPreview() { FintraceComponentPreview(false) { AmountInput("2450.00", {}) } }
@Preview(name = "Amount input · Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable private fun AmountInputDarkPreview() { FintraceComponentPreview(true) { AmountInput("", {}, error = "Enter an amount greater than zero") } }
