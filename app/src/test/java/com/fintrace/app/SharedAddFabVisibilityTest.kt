package com.fintrace.app

import com.fintrace.app.ui.navigation.Screen
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedAddFabVisibilityTest {
    @Test
    fun sharedAddFabIsVisibleOnlyOnHomeAndTransactions() {
        assertTrue(shouldShowSharedAddTransactionFab(Screen.Dashboard.route))
        assertTrue(shouldShowSharedAddTransactionFab(Screen.Transactions.route))
        assertFalse(shouldShowSharedAddTransactionFab(Screen.Analytics.route))
        assertFalse(shouldShowSharedAddTransactionFab(Screen.Categories.route))
        assertFalse(shouldShowSharedAddTransactionFab(null))
    }
}
