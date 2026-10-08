package com.fintrace.app

import com.fintrace.app.data.local.entity.CategoryEntity
import com.fintrace.app.data.local.entity.PaymentModeEntity
import com.fintrace.app.data.local.entity.TransactionEntity
import com.fintrace.app.data.local.entity.TransactionSplitEntity
import com.fintrace.app.data.local.relation.TransactionWithDetails
import com.fintrace.app.data.model.ParseConfidence
import com.fintrace.app.data.model.PaymentModeType
import com.fintrace.app.data.model.TransactionStatus
import com.fintrace.app.data.repository.FinanceRepository
import com.fintrace.app.ui.transactions.AddEditTransactionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@OptIn(ExperimentalCoroutinesApi::class)
class AddEditTransactionViewModelTest {
    private val categories = listOf(CategoryEntity(id = 1, name = "Food", colorHex = "#008877"), CategoryEntity(id = 7, name = "Other", colorHex = "#334455"))
    private val modes = listOf(PaymentModeEntity(id = 2, name = "UPI", type = PaymentModeType.UPI))

    @Test fun loadingSameIdDoesNotResetAnEditedDraft() = withMain {
        val repo = Repo()
        val vm = AddEditTransactionViewModel(repo.repository)
        vm.loadTransaction(0)
        vm.onDescriptionChange("Coffee")
        vm.loadTransaction(0)
        assertEquals("Coffee", vm.uiState.value.description)
        assertTrue(vm.uiState.value.isDirty)
    }

    @Test fun lateReferenceDataDoesNotOverwriteUserSelectionAndDefaultsAreNotDirty() = withMain {
        val categoryFlow = MutableStateFlow(emptyList<CategoryEntity>())
        val modeFlow = MutableStateFlow(emptyList<PaymentModeEntity>())
        val repo = Repo(categoryFlow, modeFlow)
        val vm = AddEditTransactionViewModel(repo.repository)
        vm.loadTransaction(0)
        vm.onCategorySelect(7)
        categoryFlow.value = categories
        modeFlow.value = modes
        assertEquals(7L, vm.uiState.value.selectedCategoryId)
        assertEquals(2L, vm.uiState.value.selectedPaymentModeId)
        assertTrue(vm.uiState.value.isDirty)

        val vmWithDefaults = AddEditTransactionViewModel(Repo().repository)
        assertEquals(categories, vmWithDefaults.categories.value)
        assertEquals(modes, vmWithDefaults.paymentModes.value)
        vmWithDefaults.loadTransaction(0)
        assertFalse(vmWithDefaults.uiState.value.isDirty)
        vmWithDefaults.onDescriptionChange("Coffee")
        vmWithDefaults.onOriginalAmountChange("5")
        assertEquals(1L, vmWithDefaults.uiState.value.selectedCategoryId)
        assertEquals(2L, vmWithDefaults.uiState.value.selectedPaymentModeId)
        assertTrue(vmWithDefaults.uiState.value.canSave)
    }

    @Test fun editCopiesMetadataAndPendingSmsEditSavesConfirmedWithCurrencyAndRecurrence() = withMain {
        val original = TransactionEntity(
            id = 42, description = "Bank / Card Expense", timestamp = 1_700_000_000_000,
            originalAmount = 50.0, myShareAmount = 50.0, categoryId = 1, paymentModeId = 2,
            smsRawBody = "SMS raw", smsSender = "BANK", smsSourceTimestamp = 77,
            status = TransactionStatus.PENDING, notes = "old", parseConfidence = ParseConfidence.RAW,
            cardLastFour = "1234", currency = "USD", isRecurring = true,
            recurringSeriesId = "series-a", recurringMonth = "2023-11", recurringDayOfMonth = 30
        )
        val repo = Repo(details = TransactionWithDetails(original, null, null))
        val vm = AddEditTransactionViewModel(repo.repository)
        vm.loadTransaction(42)
        assertFalse(vm.uiState.value.isDirty)
        vm.onDescriptionChange("Updated merchant")
        assertTrue(vm.uiState.value.isDirty)
        vm.saveTransaction()
        val saved = repo.saved.get()!!
        assertEquals(TransactionStatus.CONFIRMED, saved.status)
        assertEquals("Updated merchant", saved.description)
        assertEquals("SMS raw", saved.smsRawBody)
        assertEquals("BANK", saved.smsSender)
        assertEquals(77L, saved.smsSourceTimestamp)
        assertEquals(ParseConfidence.RAW, saved.parseConfidence)
        assertEquals("1234", saved.cardLastFour)
        assertEquals("USD", saved.currency)
        assertEquals("series-a", saved.recurringSeriesId)
        assertEquals(30, saved.recurringDayOfMonth)
    }

    @Test fun finiteValidationAllowsZeroPersonalShareAndRejectsMalformedNanAndOverflow() = withMain {
        val vm = AddEditTransactionViewModel(Repo().repository)
        vm.loadTransaction(0)
        vm.onDescriptionChange("Dinner")
        vm.onOriginalAmountChange("100")
        vm.onCategorySelect(1)
        vm.onPaymentModeSelect(2)
        vm.onSplitToggle(true)
        vm.onMyShareAmountChange("0")
        vm.onAddParticipant()
        vm.onUpdateParticipant(0, "Alex", "100")
        assertTrue(vm.uiState.value.canSave)
        vm.onOriginalAmountChange("NaN")
        assertFalse(vm.uiState.value.canSave)
        vm.onOriginalAmountChange("1e309")
        assertFalse(vm.uiState.value.canSave)
        vm.onOriginalAmountChange("100")
        vm.onUpdateParticipant(0, "Alex", "101")
        assertFalse(vm.uiState.value.canSave)
    }

    @Test fun thirdsPresetRoundsToCentsWithoutLosingTheBillTotal() = withMain {
        val vm = AddEditTransactionViewModel(Repo().repository)
        vm.loadTransaction(0)
        vm.onDescriptionChange("Lunch")
        vm.onOriginalAmountChange("10")
        vm.onQuickSplit(3)
        assertEquals("3.33", vm.uiState.value.myShareAmount)
        assertEquals(listOf("3.33", "3.34"), vm.uiState.value.splitParticipants.map { it.shareAmount })
        assertEquals(10.0, vm.uiState.value.myShareAmount.toDouble() + vm.uiState.value.splitParticipants.sumOf { it.shareAmount.toDouble() }, 0.0001)
    }

    @Test fun personalShareTracksAutomaticRemainderUntilUserOverridesIt() = withMain {
        val automatic = AddEditTransactionViewModel(Repo().repository)
        automatic.loadTransaction(0)
        automatic.onOriginalAmountChange("100")
        automatic.onSplitToggle(true)
        automatic.onAddParticipant()
        automatic.onUpdateParticipant(0, "Alex", "30")
        assertEquals("70", automatic.uiState.value.myShareAmount)
        automatic.onOriginalAmountChange("120")
        assertEquals("90", automatic.uiState.value.myShareAmount)
        automatic.onAddParticipant()
        automatic.onUpdateParticipant(1, "Sam", "10")
        assertEquals("80", automatic.uiState.value.myShareAmount)
        automatic.onRemoveParticipant(0)
        assertEquals("110", automatic.uiState.value.myShareAmount)

        val overridden = AddEditTransactionViewModel(Repo().repository)
        overridden.loadTransaction(0)
        overridden.onOriginalAmountChange("100")
        overridden.onSplitToggle(true)
        overridden.onAddParticipant()
        overridden.onUpdateParticipant(0, "Alex", "30")
        overridden.onMyShareAmountChange("25")
        overridden.onUpdateParticipant(0, "Alex", "40")
        assertEquals("25", overridden.uiState.value.myShareAmount)
        overridden.onOriginalAmountChange("200")
        assertEquals("25", overridden.uiState.value.myShareAmount)
        overridden.onRemoveParticipant(0)
        assertEquals("25", overridden.uiState.value.myShareAmount)
    }

    @Test fun overflowingRemainderMathRetainsEditedShareAndReportsValidation() = withMain {
        val vm = AddEditTransactionViewModel(Repo().repository)
        vm.loadTransaction(0)
        vm.onOriginalAmountChange("1.7e308")
        vm.onSplitToggle(true)
        vm.onAddParticipant()
        vm.onUpdateParticipant(0, "Alex", "1e308")
        val previousRemainder = vm.uiState.value.myShareAmount
        vm.onAddParticipant()
        vm.onUpdateParticipant(1, "Sam", "1e308")

        assertEquals("1e308", vm.uiState.value.splitParticipants[1].shareAmount)
        assertEquals(previousRemainder, vm.uiState.value.myShareAmount)
        assertFalse(vm.uiState.value.participantTotalIsFinite)
        assertNotNull(vm.uiState.value.shareError)
        assertFalse(vm.uiState.value.canSave)
    }

    @Test fun genericSmsDescriptionIsEnrichedWithoutChangingOriginalMetadata() = withMain {
        val body = "Alert: You've spent INR 1,450.00 on HDFC Bank Credit Card XX4321 at SWIGGY on 18-AUG-2026. Avl Lmt: INR 85,000.00"
        val original = TransactionEntity(
            id = 58, description = "1450.00", timestamp = 1_700_000_000_000,
            originalAmount = 1450.0, myShareAmount = 1450.0, categoryId = 1, paymentModeId = 2,
            smsRawBody = body, smsSender = "HDFCBK", smsSourceTimestamp = 99,
            status = TransactionStatus.PENDING, parseConfidence = ParseConfidence.RAW,
            cardLastFour = "4321", currency = "INR"
        )
        val repo = Repo(details = TransactionWithDetails(original, null, null))
        val vm = AddEditTransactionViewModel(repo.repository)
        vm.loadTransaction(58)

        assertEquals("Swiggy", vm.uiState.value.description)
        vm.saveTransaction()
        assertEquals("Swiggy", repo.saved.get()!!.description)
        assertEquals(body, repo.saved.get()!!.smsRawBody)
        assertEquals("HDFCBK", repo.saved.get()!!.smsSender)
        assertEquals(99L, repo.saved.get()!!.smsSourceTimestamp)
        assertEquals(ParseConfidence.RAW, repo.saved.get()!!.parseConfidence)
        assertEquals("4321", repo.saved.get()!!.cardLastFour)
    }

    @Test fun duplicateSaveIsIgnoredAfterSuccessAndFailureKeepsDraft() = withMain {
        val repo = Repo()
        val vm = AddEditTransactionViewModel(repo.repository)
        vm.loadTransaction(0)
        vm.onDescriptionChange("Groceries")
        vm.onOriginalAmountChange("25")
        vm.saveTransaction()
        vm.saveTransaction()
        assertEquals(1, repo.saveCalls.get())
        assertTrue(vm.uiState.value.isSaved)

        val failedRepo = Repo(failSave = true)
        val failedVm = AddEditTransactionViewModel(failedRepo.repository)
        failedVm.loadTransaction(0)
        failedVm.onDescriptionChange("Still here")
        failedVm.onOriginalAmountChange("30")
        failedVm.saveTransaction()
        assertEquals("Still here", failedVm.uiState.value.description)
        assertTrue(failedVm.uiState.value.isDirty)
        assertFalse(failedVm.uiState.value.isSaving)
        assertNotNull(failedVm.uiState.value.errorMessage)
    }

    private fun withMain(block: suspend () -> Unit) {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        try { kotlinx.coroutines.runBlocking { block() } } finally { Dispatchers.resetMain() }
    }

    private inner class Repo(
        categoryFlow: MutableStateFlow<List<CategoryEntity>> = MutableStateFlow(categories),
        modeFlow: MutableStateFlow<List<PaymentModeEntity>> = MutableStateFlow(modes),
        private val details: TransactionWithDetails? = null,
        private val failSave: Boolean = false
    ) {
        val saved = AtomicReference<TransactionEntity?>()
        val saveCalls = AtomicInteger()
        val repository: FinanceRepository = Proxy.newProxyInstance(
            FinanceRepository::class.java.classLoader, arrayOf(FinanceRepository::class.java)
        ) { _, method, args ->
            when (method.name) {
                "getAllCategories" -> categoryFlow
                "getAllPaymentModes" -> modeFlow
                "getTransactionById" -> details
                "saveTransaction" -> {
                    saveCalls.incrementAndGet()
                    saved.set(args!![0] as TransactionEntity)
                    if (failSave) error("write failed")
                    42L
                }
                "deleteTransaction" -> Unit
                "getConfirmedTransactions", "getPendingTransactions", "getDismissedTransactions", "getTransactionsForRange", "getAllCardMappings", "getCategoryBreakdown" -> flowOf(emptyList<Any>())
                "getPendingCount" -> flowOf(0)
                "getBudgetForMonth" -> flowOf(null)
                "toString" -> "Add/Edit repository test double"
                "hashCode" -> 0
                "equals" -> false
                else -> error("Unexpected repository method ${method.name}")
            }
        } as FinanceRepository
    }
}
