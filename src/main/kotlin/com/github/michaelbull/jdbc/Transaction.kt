package com.github.michaelbull.jdbc

import com.github.michaelbull.jdbc.context.CoroutineTransaction
import com.github.michaelbull.jdbc.context.connection
import com.github.michaelbull.jdbc.context.transaction
import kotlinx.coroutines.withContext
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.coroutineContext

suspend inline fun <T> transaction(crossinline block: suspend () -> T): T {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }

    val existingTransaction = coroutineContext.transaction

    return when {
        existingTransaction == null -> withContext(CoroutineTransaction()) {
            withConnection {
                execute(block)
            }
        }
        existingTransaction.isRunning -> block()
        else -> error("Attempted to start new transaction within: $existingTransaction")
    }
}

@PublishedApi
internal suspend inline fun <T> execute(crossinline block: suspend () -> T): T {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }

    val transaction = coroutineContext.transaction ?: error("No transaction in context")
    transaction.start()

    val connection = coroutineContext.connection
    connection.autoCommit = false

    return try {
        block().also {
            transaction.complete()
            connection.commit()
        }
    } catch (ex: Throwable) {
        connection.rollbackCatching()
        throw ex
    }
}
