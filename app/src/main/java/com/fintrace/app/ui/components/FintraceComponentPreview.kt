package com.fintrace.app.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.fintrace.app.ui.theme.FinanceTrackerTheme

@Composable
internal fun FintraceComponentPreview(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    FinanceTrackerTheme(darkTheme = darkTheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground
        ) {
            content()
        }
    }
}
