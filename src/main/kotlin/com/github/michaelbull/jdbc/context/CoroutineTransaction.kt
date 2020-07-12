package com.github.michaelbull.jdbc.context

import com.github.michaelbull.jdbc.TransactionState
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

@PublishedApi
internal val CoroutineContext.transaction: CoroutineTransaction?
    get() = get(CoroutineTransaction)

@PublishedApi
internal class CoroutineTransaction : AbstractCoroutineContextElement(CoroutineTransaction) {

    companion object Key : CoroutineContext.Key<CoroutineTransaction>

    var state: TransactionState = TransactionState.Idle
        private set

    val isRunning: Boolean
        get() = state == TransactionState.Running

    fun start() {
        check(state == TransactionState.Idle) { "cannot start: $this" }
        state = TransactionState.Running
    }

    fun complete() {
        check(state == TransactionState.Running) { "cannot complete: $this" }
        state = TransactionState.Completed
    }

    override fun toString(): String = "CoroutineTransaction(state=$state)"
}
