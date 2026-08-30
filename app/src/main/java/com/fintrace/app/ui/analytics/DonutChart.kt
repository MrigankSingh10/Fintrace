package com.fintrace.app.ui.analytics

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fintrace.app.data.local.relation.CategorySpendSummary
import com.fintrace.app.ui.components.formatCurrency
import com.fintrace.app.ui.components.parseColorHex
import kotlin.math.atan2

@Composable
fun DonutChart(
    categories: List<CategorySpendSummary>,
    totalSpent: Double,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp,
    strokeWidth: Dp = 28.dp,
    selectedCategory: CategorySpendSummary? = null,
    onCategoryClick: ((CategorySpendSummary?) -> Unit)? = null
) {
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(categories, totalSpent) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    val surfaceBg = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(strokeWidth / 2)
                .pointerInput(categories, totalSpent) {
                    detectTapGestures { tapOffset ->
                        if (categories.isEmpty() || totalSpent <= 0.0) return@detectTapGestures

                        val center = Offset(this@pointerInput.size.width / 2f, this@pointerInput.size.height / 2f)
                        val angle = (Math.toDegrees(
                            atan2(
                                (tapOffset.y - center.y).toDouble(),
                                (tapOffset.x - center.x).toDouble()
                            )
                        ) + 360 + 90) % 360

                        var cumulative = 0.0
                        var clicked: CategorySpendSummary? = null
                        for (cat in categories) {
                            val sweep = (cat.totalMyShareSpent / totalSpent) * 360.0
                            if (angle >= cumulative && angle < cumulative + sweep) {
                                clicked = cat
                                break
                            }
                            cumulative += sweep
                        }
                        onCategoryClick?.invoke(clicked)
                    }
                }
        ) {
            val strokePx = strokeWidth.toPx()
            val canvasSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            val topLeft = Offset(strokePx / 2, strokePx / 2)

            if (categories.isEmpty() || totalSpent <= 0.0) {
                // Empty Track
                drawArc(
                    color = surfaceBg,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = canvasSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            } else {
                var startAngle = -90f

                categories.forEach { cat ->
                    val sweepAngle = ((cat.totalMyShareSpent / totalSpent) * 360.0).toFloat() * animationProgress.value
                    val isSelected = selectedCategory?.categoryId == cat.categoryId
                    val color = parseColorHex(cat.colorHex)
                    val effectiveStroke = if (isSelected) strokePx * 1.25f else strokePx

                    if (sweepAngle > 0.5f) {
                        drawArc(
                            color = color,
                            startAngle = startAngle,
                            sweepAngle = (sweepAngle - 2f).coerceAtLeast(1f), // Small gap between slices
                            useCenter = false,
                            topLeft = topLeft,
                            size = canvasSize,
                            style = Stroke(width = effectiveStroke, cap = StrokeCap.Round)
                        )
                    }
                    startAngle += sweepAngle
                }
            }
        }

        // Center Content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            val activeCategory = selectedCategory
            if (activeCategory != null) {
                Text(
                    text = activeCategory.categoryName,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = parseColorHex(activeCategory.colorHex)
                )
                Text(
                    text = formatCurrency(activeCategory.totalMyShareSpent),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${String.format("%.1f", activeCategory.percentageOfTotal)}% of spend",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "TOTAL SPENT",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.5.sp, fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatCurrency(totalSpent),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${categories.size} categories",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
