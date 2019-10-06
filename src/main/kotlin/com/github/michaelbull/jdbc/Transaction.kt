package com.github.michaelbull.jdbc

import com.github.michaelbull.jdbc.context.CoroutineTransaction
import com.github.michaelbull.jdbc.context.connection
import com.github.michaelbull.jdbc.context.transaction
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

suspend fun <T> transaction(block: suspend () -> T): T {
    val existingTransaction = coroutineContext.transaction

    return when {
        existingTransaction == null -> withContext(CoroutineTransaction()) { execute(block) }
        existingTransaction.isRunning -> block()
        else -> error("Attempted to start new transaction within: $existingTransaction")
    }
}

private suspend fun <T> execute(block: suspend () -> T): T = withConnection {
    val transaction = coroutineContext.transaction ?: error("No transaction in context")
    transaction.start()

    val connection = coroutineContext.connection
    connection.autoCommit = false

    try {
        block().also {
            transaction.complete()
            connection.commit()
        }
    } catch (ex: Throwable) {
        connection.rollbackCatching()
        throw ex
    }
}
