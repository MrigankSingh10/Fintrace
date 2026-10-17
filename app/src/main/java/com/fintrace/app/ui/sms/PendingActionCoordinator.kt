package com.fintrace.app.ui.sms

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.fintrace.app.data.model.TransactionStatus

internal fun isPendingForDeferredCommit(status: TransactionStatus?): Boolean = status == TransactionStatus.PENDING

internal enum class PendingActionPhase { UNDOABLE, COMMITTING }

internal data class PendingActionRecord(
    val token: Long,
    val action: PendingAction,
    val description: String,
    val phase: PendingActionPhase
)

/** Serializes stage, Undo, and expiry transitions by action token. All public state changes are atomic. */
internal class PendingActionCoordinator(
    private val scope: CoroutineScope,
    private val delayMillis: Long = 5_000L,
    private val wait: suspend (Long) -> Unit = { delay(it) },
    private val commit: suspend (Long, PendingAction) -> Unit,
    private val onError: (Throwable) -> Unit
) {
    private val lock = Any()
    private var nextToken = 0L
    private val mutableActions = MutableStateFlow<Map<Long, PendingActionRecord>>(emptyMap())
    // This watermark deliberately survives expiry/Undo. It prevents an older hidden action
    // from becoming visible again after the newest banner disappears.
    private val mutableNewestBannerToken = MutableStateFlow<Long?>(null)
    val newestBannerToken: StateFlow<Long?> = mutableNewestBannerToken.asStateFlow()
    val actions: StateFlow<Map<Long, PendingActionRecord>> = mutableActions.asStateFlow()
    private val jobs = mutableMapOf<Long, TrackedJob>()

    fun stage(id: Long, action: PendingAction, description: String): Boolean {
        lateinit var job: Job
        val token: Long
        synchronized(lock) {
            if (mutableActions.value.containsKey(id)) return false
            token = ++nextToken
            mutableNewestBannerToken.value = token
            mutableActions.value = mutableActions.value + (id to PendingActionRecord(token, action, description, PendingActionPhase.UNDOABLE))
            job = scope.launch(start = CoroutineStart.LAZY) {
                try {
                    wait(delayMillis)
                    val mayCommit = synchronized(lock) {
                        val current = mutableActions.value[id]
                        if (current?.token != token || current.phase != PendingActionPhase.UNDOABLE) false
                        else {
                            mutableActions.value = mutableActions.value + (id to current.copy(phase = PendingActionPhase.COMMITTING))
                            true
                        }
                    }
                    if (!mayCommit) return@launch
                    try {
                        commit(id, action)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        onError(error)
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } finally {
                    synchronized(lock) {
                        if (mutableActions.value[id]?.token == token) {
                            mutableActions.value = mutableActions.value - id
                        }
                        if (jobs[id]?.let { it.token == token && it.job === job } == true) jobs.remove(id)
                    }
                }
            }
            jobs[id] = TrackedJob(token, job)
        }
        job.start()
        return true
    }

    /** Returns true only when Undo won the race while the action was still undoable. */
    fun undo(id: Long): Boolean {
        val jobToCancel = synchronized(lock) {
            val current = mutableActions.value[id] ?: return false
            if (current.phase != PendingActionPhase.UNDOABLE || current.token != mutableNewestBannerToken.value) return false
            mutableActions.value = mutableActions.value - id
            jobs[id]?.takeIf { it.token == current.token }?.also { jobs.remove(id) }?.job
        }
        jobToCancel?.cancel()
        return true
    }

    fun undoableRecords(): List<Pair<Long, PendingActionRecord>> = actions.value.entries
        .filter { it.value.phase == PendingActionPhase.UNDOABLE }
        .map { it.key to it.value }

    fun visibleUndoableRecord(): Pair<Long, PendingActionRecord>? = synchronized(lock) {
        val newestToken = mutableNewestBannerToken.value ?: return@synchronized null
        mutableActions.value.entries.firstOrNull {
            it.value.token == newestToken && it.value.phase == PendingActionPhase.UNDOABLE
        }?.let { it.key to it.value }
    }

    fun activeIds(): Set<Long> = actions.value.keys

    private data class TrackedJob(val token: Long, val job: Job)
}
