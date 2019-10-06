package com.github.michaelbull.jdbc.context

import com.github.michaelbull.jdbc.TransactionState
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

val CoroutineContext.transaction: CoroutineTransaction?
    get() = get(CoroutineTransaction)

class CoroutineTransaction : AbstractCoroutineContextElement(CoroutineTransaction) {

    companion object Key : CoroutineContext.Key<CoroutineTransaction>

    private var state: TransactionState = TransactionState.Idle

    internal val isRunning: Boolean
        get() = state == TransactionState.Running

    internal fun start() {
        check(state == TransactionState.Idle) { "cannot start: $this" }
        state = TransactionState.Running
    }

    internal fun complete() {
        check(state == TransactionState.Running) { "cannot complete: $this" }
        state = TransactionState.Completed
    }

    override fun toString(): String = "CoroutineTransaction(state=$state)"
}
