package com.fintrace.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.fintrace.app.FinanceTrackerApp
import com.fintrace.app.data.repository.FinanceRepository
import com.fintrace.app.ui.analytics.AnalyticsScreen
import com.fintrace.app.ui.analytics.AnalyticsViewModel
import com.fintrace.app.ui.categories.CategoryListScreen
import com.fintrace.app.ui.categories.CategoryViewModel
import com.fintrace.app.ui.dashboard.DashboardScreen
import com.fintrace.app.ui.dashboard.DashboardViewModel
import com.fintrace.app.ui.paymentmodes.PaymentModeListScreen
import com.fintrace.app.ui.paymentmodes.PaymentModeViewModel
import com.fintrace.app.ui.sms.SmsInboxScreen
import com.fintrace.app.ui.sms.SmsInboxViewModel
import com.fintrace.app.ui.transactions.AddEditTransactionScreen
import com.fintrace.app.ui.transactions.AddEditTransactionViewModel
import com.fintrace.app.ui.transactions.TransactionListScreen
import com.fintrace.app.ui.transactions.TransactionListViewModel

/** Simple factory to create a ViewModel with a single [FinanceRepository] constructor arg. */
@Suppress("UNCHECKED_CAST")
private inline fun <reified VM : ViewModel> repositoryFactory(
    repository: FinanceRepository
): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AddEditTransactionViewModel::class.java) ->
                AddEditTransactionViewModel(repository) as T
            modelClass.isAssignableFrom(TransactionListViewModel::class.java) ->
                TransactionListViewModel(repository) as T
            modelClass.isAssignableFrom(CategoryViewModel::class.java) ->
                CategoryViewModel(repository) as T
            modelClass.isAssignableFrom(PaymentModeViewModel::class.java) ->
                PaymentModeViewModel(repository) as T
            modelClass.isAssignableFrom(DashboardViewModel::class.java) ->
                DashboardViewModel(repository) as T
            modelClass.isAssignableFrom(AnalyticsViewModel::class.java) ->
                AnalyticsViewModel(repository) as T
            modelClass.isAssignableFrom(SmsInboxViewModel::class.java) ->
                SmsInboxViewModel(repository, FinanceTrackerApp.instance.database) as T
            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    repository: FinanceRepository,
    modifier: Modifier = Modifier
) {
    // Shared ViewModels — one instance per NavGraph lifetime (fine for screens that
    // don't mutate per-item state). AddEditTransactionViewModel is intentionally excluded.
    val factory = repositoryFactory<ViewModel>(repository)
    val categoryViewModel: CategoryViewModel = viewModel(factory = factory)
    val paymentModeViewModel: PaymentModeViewModel = viewModel(factory = factory)
    val transactionListViewModel: TransactionListViewModel = viewModel(factory = factory)
    val dashboardViewModel: DashboardViewModel = viewModel(factory = factory)
    val smsInboxViewModel: SmsInboxViewModel = viewModel(factory = factory)
    val analyticsViewModel: AnalyticsViewModel = viewModel(factory = factory)

    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                viewModel = dashboardViewModel,
                onNavigateToAddTransaction = {
                    navController.navigate(Screen.AddTransaction.createRoute(0L))
                },
                onNavigateToTransactions = {
                    navController.navigate(Screen.Transactions.route)
                },
                onNavigateToCategories = {
                    navController.navigate(Screen.Categories.route)
                },
                onNavigateToPaymentModes = {
                    navController.navigate(Screen.PaymentModes.route)
                },
                onNavigateToTransactionDetail = { transactionId ->
                    navController.navigate(Screen.AddTransaction.createRoute(transactionId))
                }
            )
        }

        composable(Screen.Transactions.route) {
            TransactionListScreen(
                viewModel = transactionListViewModel,
                onNavigateToAddTransaction = {
                    navController.navigate(Screen.AddTransaction.createRoute(0L))
                },
                onNavigateToEditTransaction = { transactionId ->
                    navController.navigate(Screen.AddTransaction.createRoute(transactionId))
                }
            )
        }

        composable(Screen.SmsInbox.route) {
            SmsInboxScreen(
                viewModel = smsInboxViewModel,
                onNavigateToEditTransaction = { transactionId ->
                    navController.navigate(Screen.AddTransaction.createRoute(transactionId))
                }
            )
        }

        composable(Screen.Analytics.route) {
            AnalyticsScreen(
                viewModel = analyticsViewModel
            )
        }

        composable(Screen.Categories.route) {
            CategoryListScreen(
                viewModel = categoryViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.PaymentModes.route) {
            PaymentModeListScreen(
                viewModel = paymentModeViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.AddTransaction.route,
            arguments = listOf(
                navArgument("transactionId") {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            )
        ) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getLong("transactionId") ?: 0L
            // Create a FRESH ViewModel scoped to this back-stack entry.
            // This ensures Add and Edit never share stale state.
            val addEditViewModel: AddEditTransactionViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = factory
            )
            AddEditTransactionScreen(
                transactionId = transactionId,
                viewModel = addEditViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
