package com.fintrace.app.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.IntOffset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fintrace.app.data.model.TransactionType
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SwipeableReviewCard(
    title: String,
    amount: Double,
    smsBody: String?,
    modifier: Modifier = Modifier,
    sender: String? = null,
    transactionType: TransactionType = TransactionType.EXPENSE,
    dateLabel: String? = null,
    categoryLabel: String? = null,
    paymentLabel: String? = null,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onCategoryClick: () -> Unit = {},
    onPaymentClick: () -> Unit = {},
    onEdit: () -> Unit = {},
    currencyCode: String = "INR",
    customContent: (@Composable () -> Unit)? = null
) {
    var smsExpanded by remember { mutableStateOf(false) }
    val thresholdPx = with(LocalDensity.current) { 96.dp.toPx() }
    var dragTotal by remember { mutableFloatStateOf(0f) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var actionInvoked by remember { mutableStateOf(false) }
    val gestureModifier = Modifier.pointerInput(onConfirm, onDismiss, thresholdPx) {
        detectHorizontalDragGestures(
            onDragStart = { dragTotal = 0f; dragOffset = 0f; actionInvoked = false },
            onHorizontalDrag = { _, dragAmount ->
                if (!actionInvoked) {
                    dragTotal += dragAmount
                    dragOffset = dragTotal.coerceIn(-size.width * 0.6f, size.width * 0.6f)
                    if (dragTotal >= thresholdPx) {
                        actionInvoked = true
                        onConfirm()
                    } else if (dragTotal <= -thresholdPx) {
                        actionInvoked = true
                        onDismiss()
                    }
                }
            },
            onDragEnd = { dragTotal = 0f; dragOffset = 0f; actionInvoked = false },
            onDragCancel = { dragTotal = 0f; dragOffset = 0f; actionInvoked = false }
        )
    }
    Box(modifier = modifier.fillMaxWidth().then(gestureModifier)) {
        if (dragOffset != 0f) {
            val confirming = dragOffset > 0f
            val feedbackColor = if (confirming) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
            val feedbackTextColor = if (confirming) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
            Box(Modifier.matchParentSize().background(feedbackColor, RoundedCornerShape(20.dp)))
            Row(
                modifier = Modifier.align(if (confirming) Alignment.CenterStart else Alignment.CenterEnd).padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.Icon(
                    if (confirming) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = null,
                    tint = feedbackTextColor
                )
                Text(if (confirming) "Confirm" else "Dismiss", color = feedbackTextColor, style = MaterialTheme.typography.labelLarge)
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth().offset { IntOffset(dragOffset.roundToInt(), 0) }
                .fintraceCardBorder(RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            if (customContent != null) {
                customContent()
            } else Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(transactionType.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            dateLabel?.let { Text("· $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                        sender?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    MoneyText(
                        amount,
                        tone = when (transactionType) {
                            TransactionType.INCOME -> MoneyTone.Income
                            TransactionType.EXPENSE -> MoneyTone.Expense
                            TransactionType.TRANSFER -> MoneyTone.Neutral
                        },
                        sign = when (transactionType) {
                            TransactionType.INCOME -> "+"
                            TransactionType.EXPENSE -> "−"
                            TransactionType.TRANSFER -> null
                        },
                        currencyCode = currencyCode,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onCategoryClick, modifier = Modifier.heightIn(min = 48.dp)) { Text(categoryLabel ?: "Choose category") }
                    OutlinedButton(onClick = onPaymentClick, modifier = Modifier.heightIn(min = 48.dp)) { Text(paymentLabel ?: "Payment mode") }
                    OutlinedButton(onClick = onEdit, modifier = Modifier.heightIn(min = 48.dp)) {
                        androidx.compose.material3.Icon(Icons.Default.Edit, contentDescription = null)
                        Text("Edit")
                    }
                }
                if (!smsBody.isNullOrBlank()) {
                    TextButton(onClick = { smsExpanded = !smsExpanded }) {
                        androidx.compose.material3.Icon(Icons.Default.ExpandMore, contentDescription = null)
                        Text(if (smsExpanded) "Hide message" else "View message")
                    }
                    if (smsExpanded) Text(smsBody, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                        androidx.compose.material3.Icon(Icons.Default.Close, contentDescription = null)
                        Text("Dismiss")
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.heightIn(min = 48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        androidx.compose.material3.Icon(Icons.Default.Check, contentDescription = null)
                        Text("Confirm")
                    }
                }
            }
        }
    }
}

@Preview(name = "Review card · Light", showBackground = true)
@Composable private fun SwipeableReviewCardLightPreview() { FintraceComponentPreview(false) { SwipeableReviewCard("Cafe Coffee Day", 485.0, "Rs. 485.00 spent at Cafe Coffee Day using UPI.", sender = "HDFCBK", transactionType = TransactionType.EXPENSE, dateLabel = "Jun 18, 8:45 PM", onConfirm = {}, onDismiss = {}, modifier = Modifier.padding(8.dp)) } }
@Preview(name = "Review card · Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable private fun SwipeableReviewCardDarkPreview() { FintraceComponentPreview(true) { SwipeableReviewCard("Online purchase", 12899.0, null, sender = "VISA •••• 4218", onConfirm = {}, onDismiss = {}, modifier = Modifier.padding(8.dp)) } }
