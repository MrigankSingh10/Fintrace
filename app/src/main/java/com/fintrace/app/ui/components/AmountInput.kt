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
import androidx.compose.ui.text.input.KeyboardType
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
            textAlign = TextAlign.Center
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    )
}

@Preview(name = "Amount input · Light", showBackground = true)
@Composable private fun AmountInputLightPreview() { FintraceComponentPreview(false) { AmountInput("2,450.00", {}) } }
@Preview(name = "Amount input · Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable private fun AmountInputDarkPreview() { FintraceComponentPreview(true) { AmountInput("", {}, error = "Enter an amount greater than zero") } }
