package com.fintrace.app.ui.components

import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** Light cards get the spec's hairline outline; dark cards stay tonal and borderless. */
@Composable
internal fun Modifier.fintraceCardBorder(shape: Shape): Modifier =
    if (MaterialTheme.colorScheme.background.luminance() >= 0.5f) {
        border(1.dp, MaterialTheme.colorScheme.outline, shape)
    } else {
        this
    }
