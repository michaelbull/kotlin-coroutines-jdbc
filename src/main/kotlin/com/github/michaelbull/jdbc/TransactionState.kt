package com.github.michaelbull.jdbc

/**
 * Represents the finite states of a transaction.
 */
internal sealed class TransactionState {

    /**
     * A transaction that has been created, but has not started running.
     */
    object Idle : TransactionState()

    /**
     * A transaction that has been created and has started running, but not yet completed.
     */
    object Running : TransactionState()

    /**
     * A transaction that has been created, ran, and has either produced the result to commit or thrown an exception.
     */
    object Completed : TransactionState()
}
