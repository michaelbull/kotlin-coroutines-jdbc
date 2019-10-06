package com.github.michaelbull.jdbc

internal sealed class TransactionState {
    object Idle : TransactionState()
    object Running : TransactionState()
    object Completed : TransactionState()
}
