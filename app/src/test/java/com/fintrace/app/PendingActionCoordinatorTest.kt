package com.fintrace.app

import com.fintrace.app.ui.sms.PendingAction
import com.fintrace.app.ui.sms.PendingActionCoordinator
import com.fintrace.app.ui.sms.PendingActionPhase
import com.fintrace.app.ui.sms.isPendingForDeferredCommit
import com.fintrace.app.data.model.TransactionStatus
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class PendingActionCoordinatorTest {
    @Test fun undoBeforeExpiryPreventsCommit() = runTest {
        val committed = mutableListOf<Long>()
        val coordinator = PendingActionCoordinator(this, commit = { id, _ -> committed += id }, onError = {})
        assertTrue(coordinator.stage(4, PendingAction.CONFIRM, "Cafe"))
        runCurrent()
        assertTrue(coordinator.undo(4))
        advanceTimeBy(5_000)
        runCurrent()
        assertTrue(committed.isEmpty())
        assertTrue(coordinator.actions.value.isEmpty())
    }

    @Test fun expiryRemovesUndoBeforeFetchAndUndoCannotClaimCommitReversal() = runTest {
        val commitEntered = CompletableDeferred<Unit>()
        val allowCommit = CompletableDeferred<Unit>()
        lateinit var coordinator: PendingActionCoordinator
        coordinator = PendingActionCoordinator(this, commit = { _, _ ->
            commitEntered.complete(Unit)
            allowCommit.await()
        }, onError = {})
        coordinator.stage(9, PendingAction.DISMISS, "Market")
        runCurrent()
        advanceTimeBy(5_000)
        runCurrent()
        commitEntered.await()
        assertEquals(PendingActionPhase.COMMITTING, coordinator.actions.value[9]?.phase)
        assertFalse(coordinator.undo(9))
        allowCommit.complete(Unit)
        runCurrent()
        assertTrue(coordinator.actions.value.isEmpty())
    }

    @Test fun oldCancelledJobCannotClearImmediatelyRestagedAction() = runTest {
        val commitEntered = CompletableDeferred<Unit>()
        val allowCommit = CompletableDeferred<Unit>()
        val coordinator = PendingActionCoordinator(this, commit = { _, _ -> commitEntered.complete(Unit); allowCommit.await() }, onError = {})
        coordinator.stage(11, PendingAction.CONFIRM, "Cafe")
        runCurrent()
        val firstToken = coordinator.actions.value.getValue(11).token
        assertTrue(coordinator.undo(11))
        assertTrue(coordinator.stage(11, PendingAction.DISMISS, "Cafe"))
        val secondToken = coordinator.actions.value.getValue(11).token
        assertTrue(secondToken > firstToken)
        runCurrent() // old cancellation cleanup and new timer startup
        assertEquals(secondToken, coordinator.actions.value[11]?.token)
        advanceTimeBy(5_000)
        runCurrent()
        commitEntered.await()
        assertEquals(PendingActionPhase.COMMITTING, coordinator.actions.value[11]?.phase)
        allowCommit.complete(Unit)
    }

    @Test fun staleNonPendingRowSkipsTheWriteAndCommitErrorRestoresPendingRow() = runTest {
        var writes = 0
        val errors = mutableListOf<String>()
        val coordinator = PendingActionCoordinator(this, commit = { _, _ ->
            // The ViewModel applies this policy to the row returned by the latest repository read.
            if (isPendingForDeferredCommit(TransactionStatus.CONFIRMED)) writes++
        }, onError = { errors += it.message.orEmpty() })
        coordinator.stage(20, PendingAction.CONFIRM, "Changed elsewhere")
        runCurrent()
        advanceTimeBy(5_000)
        runCurrent()
        assertEquals(0, writes)
        assertTrue(coordinator.actions.value.isEmpty())

        val failing = PendingActionCoordinator(this, commit = { _, _ -> error("storage failed") }, onError = { errors += it.message.orEmpty() })
        failing.stage(21, PendingAction.DISMISS, "Keep")
        runCurrent()
        advanceTimeBy(5_000)
        runCurrent()
        assertTrue(failing.actions.value.isEmpty())
        assertEquals(listOf("storage failed"), errors)
        assertFalse(isPendingForDeferredCommit(null))
        assertTrue(isPendingForDeferredCommit(TransactionStatus.PENDING))
    }
}
