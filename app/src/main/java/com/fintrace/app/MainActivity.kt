package com.fintrace.app

import android.Manifest
import android.content.res.Configuration
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.fintrace.app.ui.navigation.AppBottomNavBar
import com.fintrace.app.ui.navigation.AppNavGraph
import com.fintrace.app.ui.navigation.Screen
import com.fintrace.app.ui.sms.SmsPermissionRationaleDialog
import com.fintrace.app.ui.theme.FinanceTrackerTheme
import java.time.YearMonth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as FinanceTrackerApp
        val systemDarkTheme =
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        val initialDarkTheme = app.isDarkTheme(systemDarkTheme)
        applyEdgeToEdge(initialDarkTheme)

        setContent {
            var isDarkTheme by remember { mutableStateOf(initialDarkTheme) }

            SideEffect {
                applyEdgeToEdge(isDarkTheme)
            }

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

private fun ComponentActivity.applyEdgeToEdge(darkTheme: Boolean) {
    enableEdgeToEdge(
        statusBarStyle = SystemBarStyle.auto(
            lightScrim = android.graphics.Color.TRANSPARENT,
            darkScrim = android.graphics.Color.TRANSPARENT,
            detectDarkMode = { darkTheme }
        ),
        navigationBarStyle = SystemBarStyle.auto(
            lightScrim = 0xE6FFFFFF.toInt(),
            darkScrim = 0x801B1B1B.toInt(),
            detectDarkMode = { darkTheme }
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    app: FinanceTrackerApp,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val permissionPreferences = remember(context) {
        context.getSharedPreferences("sms_permission_preferences", android.content.Context.MODE_PRIVATE)
    }
    fun hasSmsPermissions(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED

    var showStartupSmsPermissionRationale by rememberSaveable {
        mutableStateOf(
            !hasSmsPermissions() &&
                !permissionPreferences.getBoolean(STARTUP_SMS_PROMPT_HANDLED_KEY, false)
        )
    }
    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { /* Grant state is read from PackageManager where SMS review needs it. */ }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isMainTab = currentRoute in Screen.primaryTabs.map { it.route }
    val showTopAppBar = isMainTab && currentRoute != Screen.Dashboard.route
    val showSharedAddFab = shouldShowSharedAddTransactionFab(currentRoute)
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
    val exportReport = {
        exportRequest += 1
        if (currentRoute != Screen.Analytics.route) {
            navController.navigate(Screen.Analytics.route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    Scaffold(
        topBar = {
            if (showTopAppBar) {
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
                        MainOverflowMenuButton(
                            expanded = menuExpanded,
                            onExpandedChange = { menuExpanded = it },
                            isDarkTheme = isDarkTheme,
                            pendingSmsCount = pendingSmsCount,
                            onSettings = { navController.navigate(Screen.Settings.route) },
                            onExportReport = exportReport,
                            onThemeToggle = onThemeToggle,
                            onSmsReview = { navController.navigate(Screen.SmsInbox.route) }
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        },
        floatingActionButton = {
            if (showSharedAddFab) {
                ExtendedFloatingActionButton(
                    onClick = { navController.navigate(Screen.AddTransaction.createRoute(0L)) },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Add") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End,
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
                onExportRequestConsumed = { exportRequest = 0 },
                homeActions = {
                    MainOverflowMenuButton(
                        expanded = menuExpanded,
                        onExpandedChange = { menuExpanded = it },
                        isDarkTheme = isDarkTheme,
                        pendingSmsCount = pendingSmsCount,
                        onSettings = { navController.navigate(Screen.Settings.route) },
                        onExportReport = exportReport,
                        onThemeToggle = onThemeToggle,
                        onSmsReview = { navController.navigate(Screen.SmsInbox.route) }
                    )
                }
            )
        }
    }

    if (showStartupSmsPermissionRationale) {
        SmsPermissionRationaleDialog(
            onDismiss = {
                permissionPreferences.edit().putBoolean(STARTUP_SMS_PROMPT_HANDLED_KEY, true).apply()
                showStartupSmsPermissionRationale = false
            },
            onRequestPermission = {
                smsPermissionLauncher.launch(
                    arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
                )
            }
        )
    }
}

@Composable
private fun MainOverflowMenuButton(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    isDarkTheme: Boolean,
    pendingSmsCount: Int,
    onSettings: () -> Unit,
    onExportReport: () -> Unit,
    onThemeToggle: () -> Unit,
    onSmsReview: () -> Unit
) {
    IconButton(onClick = { onExpandedChange(true) }) {
        Icon(Icons.Default.MoreVert, contentDescription = "More options")
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
        DropdownMenuItem(
            text = { Text("Settings") },
            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
            onClick = { onExpandedChange(false); onSettings() }
        )
        DropdownMenuItem(
            text = { Text("Export report") },
            leadingIcon = { Icon(Icons.Default.FileDownload, contentDescription = null) },
            onClick = { onExpandedChange(false); onExportReport() }
        )
        DropdownMenuItem(
            text = { Text(if (isDarkTheme) "Light theme" else "Dark theme") },
            leadingIcon = {
                Icon(if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode, contentDescription = null)
            },
            onClick = { onExpandedChange(false); onThemeToggle() }
        )
        DropdownMenuItem(
            text = { Text(if (pendingSmsCount > 0) "SMS Review ($pendingSmsCount)" else "SMS Review") },
            leadingIcon = { Icon(Icons.Default.MarkEmailUnread, contentDescription = null) },
            onClick = { onExpandedChange(false); onSmsReview() }
        )
    }
}

private const val STARTUP_SMS_PROMPT_HANDLED_KEY = "startup_prompt_handled"

internal fun shouldShowSharedAddTransactionFab(route: String?): Boolean =
    route == Screen.Dashboard.route || route == Screen.Transactions.route
