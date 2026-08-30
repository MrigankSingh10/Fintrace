package com.fintrace.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fintrace.app.data.local.entity.PaymentModeEntity
import com.fintrace.app.data.model.PaymentModeType
import com.fintrace.app.ui.theme.PrimaryBlue

fun getPaymentModeIcon(type: PaymentModeType): ImageVector {
    return when (type) {
        PaymentModeType.BANK_DEBIT -> Icons.Default.AccountBalance
        PaymentModeType.CREDIT_CARD -> Icons.Default.CreditCard
        PaymentModeType.UPI -> Icons.Default.Smartphone
        PaymentModeType.CASH -> Icons.Default.Payments
        PaymentModeType.OTHER -> Icons.Default.CreditCard
    }
}

@Composable
fun PaymentModeBadge(
    mode: PaymentModeEntity?,
    modifier: Modifier = Modifier,
    tint: Color = PrimaryBlue
) {
    val name = mode?.name ?: "Unknown Mode"
    val icon = mode?.let { getPaymentModeIcon(it.type) } ?: Icons.Default.CreditCard

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(tint.copy(alpha = 0.12f))
            .border(1.dp, tint.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = tint
        )
    }
}
