package com.fintrace.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.FabPosition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.fintrace.app.ui.navigation.AppBottomNavBar
import com.fintrace.app.ui.navigation.AppNavGraph
import com.fintrace.app.ui.navigation.Screen
import com.fintrace.app.ui.theme.FinanceTrackerTheme
import java.time.YearMonth

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

    val isMainTab = currentRoute in Screen.primaryTabs.map { it.route }
    val pendingSmsCount by app.repository.getPendingCount().collectAsState(initial = 0)
    val incomeModelReady by app.incomeModelReady.collectAsState()
    var sharedMonth by rememberSaveable(
        stateSaver = Saver<YearMonth, String>(
            save = { it.toString() },
            restore = { YearMonth.parse(it) }
        )
    ) { mutableStateOf(YearMonth.now()) }
    var exportRequest by rememberSaveable { mutableStateOf(0) }
    var menuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            if (isMainTab) {
                TopAppBar(
                    title = {
                        Text(
                            text = when (currentRoute) {
                                Screen.Transactions.route -> "Transactions"
                                Screen.Analytics.route -> "Analytics"
                                else -> "Fintrace"
                            },
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    actions = {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("Settings") },
                                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                onClick = { menuExpanded = false; navController.navigate(Screen.Settings.route) }
                            )
                            DropdownMenuItem(
                                text = { Text("Export report") },
                                leadingIcon = { Icon(Icons.Default.FileDownload, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    exportRequest += 1
                                    if (currentRoute != Screen.Analytics.route) {
                                        navController.navigate(Screen.Analytics.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isDarkTheme) "Light theme" else "Dark theme") },
                                leadingIcon = {
                                    Icon(if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode, contentDescription = null)
                                },
                                onClick = { menuExpanded = false; onThemeToggle() }
                            )
                            DropdownMenuItem(
                                text = { Text(if (pendingSmsCount > 0) "SMS Review ($pendingSmsCount)" else "SMS Review") },
                                leadingIcon = { Icon(Icons.Default.MarkEmailUnread, contentDescription = null) },
                                onClick = { menuExpanded = false; navController.navigate(Screen.SmsInbox.route) }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        },
        floatingActionButton = {
            if (isMainTab) {
                ExtendedFloatingActionButton(
                    onClick = { navController.navigate(Screen.AddTransaction.createRoute(0L)) },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Add") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        bottomBar = {
            if (isMainTab) {
                AppBottomNavBar(
                    navController = navController
                )
            }
        }
    ) { paddingValues ->
        // Screens below read monthly_budgets with adjustment semantics, so they are only mounted
        // once the one-time legacy conversion has finished.
        if (incomeModelReady) {
            AppNavGraph(
                navController = navController,
                repository = app.repository,
                modifier = Modifier.padding(paddingValues).consumeWindowInsets(paddingValues),
                sharedMonth = sharedMonth,
                onSharedMonthSelected = { sharedMonth = it },
                isDarkTheme = isDarkTheme,
                onThemeToggle = onThemeToggle,
                exportRequest = exportRequest,
                onRequestExport = { exportRequest += 1 },
                onExportRequestConsumed = { exportRequest = 0 }
            )
        }
    }
}
