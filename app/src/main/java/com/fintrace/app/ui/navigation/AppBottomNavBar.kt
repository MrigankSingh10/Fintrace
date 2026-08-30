package com.fintrace.app.ui.navigation

import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.fintrace.app.ui.theme.ExpenseRed
import com.fintrace.app.ui.theme.PrimaryEmerald

@Composable
fun AppBottomNavBar(
    navController: NavController,
    pendingSmsCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val currentBackStack = navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack.value?.destination?.route

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        modifier = modifier
    ) {
        Screen.bottomNavItems.forEach { screen ->
            val isSelected = currentRoute == screen.route

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (currentRoute != screen.route) {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    if (screen == Screen.SmsInbox && pendingSmsCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge(containerColor = ExpenseRed) {
                                    Text(text = "$pendingSmsCount")
                                }
                            }
                        ) {
                            screen.icon?.let { Icon(imageVector = it, contentDescription = screen.title) }
                        }
                    } else {
                        screen.icon?.let { Icon(imageVector = it, contentDescription = screen.title) }
                    }
                },
                label = {
                    Text(
                        text = screen.title,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PrimaryEmerald,
                    selectedTextColor = PrimaryEmerald,
                    indicatorColor = PrimaryEmerald.copy(alpha = 0.15f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
