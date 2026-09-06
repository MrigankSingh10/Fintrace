package com.fintrace.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fintrace.app.ui.navigation.AppBottomNavBar
import com.fintrace.app.ui.navigation.AppNavGraph
import com.fintrace.app.ui.navigation.Screen
import com.fintrace.app.ui.theme.FinanceTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as FinanceTrackerApp

        setContent {
            val systemDarkTheme = isSystemInDarkTheme()
            var isDarkTheme by remember { mutableStateOf(app.isDarkTheme(systemDarkTheme)) }

            FinanceTrackerTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        app = app,
                        isDarkTheme = isDarkTheme,
                        onThemeToggle = {
                            isDarkTheme = !isDarkTheme
                            app.setDarkTheme(isDarkTheme)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    app: FinanceTrackerApp,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isMainTab = currentRoute in Screen.bottomNavItems.map { it.route }
    val pendingSmsCount by app.repository.getPendingCount().collectAsState(initial = 0)

    Scaffold(
        topBar = {
            if (isMainTab) {
                TopAppBar(
                    title = {
                        Text(
                            text = "Finance Tracker",
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    actions = {
                        IconButton(onClick = onThemeToggle) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = if (isDarkTheme) "Switch to light theme" else "Switch to dark theme"
                            )
                        }
                        IconButton(onClick = { navController.navigate(Screen.Categories.route) }) {
                            Icon(
                                imageVector = Icons.Default.Category,
                                contentDescription = "Manage Categories"
                            )
                        }
                        IconButton(onClick = { navController.navigate(Screen.PaymentModes.route) }) {
                            Icon(
                                imageVector = Icons.Default.CreditCard,
                                contentDescription = "Manage Payment Modes"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        },
        bottomBar = {
            if (isMainTab) {
                AppBottomNavBar(
                    navController = navController,
                    pendingSmsCount = pendingSmsCount
                )
            }
        }
    ) { paddingValues ->
        AppNavGraph(
            navController = navController,
            repository = app.repository,
            modifier = Modifier.padding(paddingValues)
        )
    }
}
