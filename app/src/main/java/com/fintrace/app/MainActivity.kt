package com.fintrace.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CreditCard
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
        val repository = app.repository

        setContent {
            FinanceTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(app = app)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(app: FinanceTrackerApp) {
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
