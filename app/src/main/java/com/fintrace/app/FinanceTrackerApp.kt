package com.fintrace.app

import android.app.Application
import com.fintrace.app.data.local.AppDatabase
import com.fintrace.app.data.repository.FinanceRepository
import com.fintrace.app.data.repository.FinanceRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class FinanceTrackerApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this, applicationScope)
    }

    val repository: FinanceRepository by lazy {
        FinanceRepositoryImpl(
            categoryDao = database.categoryDao(),
            paymentModeDao = database.paymentModeDao(),
            transactionDao = database.transactionDao(),
            monthlyBudgetDao = database.monthlyBudgetDao()
        )
    }

    fun isDarkTheme(defaultValue: Boolean): Boolean =
        getSharedPreferences("display_preferences", MODE_PRIVATE)
            .getBoolean("dark_theme", defaultValue)

    fun setDarkTheme(enabled: Boolean) {
        getSharedPreferences("display_preferences", MODE_PRIVATE)
            .edit()
            .putBoolean("dark_theme", enabled)
            .apply()
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: FinanceTrackerApp
            private set
    }
}
