package com.github.michaelbull.jdbc

import com.github.michaelbull.jdbc.context.CoroutineTransaction
import com.github.michaelbull.jdbc.context.connection
import com.github.michaelbull.jdbc.context.transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import java.sql.Connection
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Calls the specified suspending [block] in the context of a [CoroutineTransaction], suspends until it completes, and
 * returns the result.
 *
 * When the [currentCoroutineContext] has no [CoroutineTransaction], the specified suspending [block] will be
 * [ran transactionally][runTransactionally] [with the context of a Connection][withConnection].
 *
 * When the [currentCoroutineContext] has an [incomplete][CoroutineTransaction.incomplete] [CoroutineTransaction], the
 * specified suspending [block] will be called within a new [coroutineScope].
 *
 * When the [currentCoroutineContext] has a [completed][CoroutineTransaction.completed] [CoroutineTransaction], an
 * [IllegalStateException] will be thrown as the transaction cannot be re-used.
 */
suspend inline fun <T> transaction(crossinline block: suspend CoroutineScope.() -> T): T {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }

    val existingTransaction = currentCoroutineContext().transaction

    return when {
        existingTransaction == null -> {
            withConnection {
                runTransactionally {
                    block()
                }
            }
        }

        existingTransaction.incomplete -> {
            coroutineScope {
                block()
            }
        }

        else -> error("Attempted to start new transaction within: $existingTransaction")
    }
}

/**
 * Calls the specified suspending [block] [with the context][withContext] of a [CoroutineTransaction] and returns its
 * result.
 *
 * If invocation of the suspending [block] was successful, [commit][Connection.commit] is then called on the
 * [Connection] in the [coroutineContext].
 *
 * If invocation of the suspending [block] throws a [Throwable] exception, [rollback][Connection.rollback] is then
 * called on the [Connection] in the [coroutineContext] and the exception is thrown.
 */
@PublishedApi
internal suspend inline fun <T> runTransactionally(crossinline block: suspend CoroutineScope.() -> T): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    currentCoroutineContext().connection.runWithManualCommit {
        val transaction = CoroutineTransaction()

        try {
            val result = withContext(transaction) {
                block()
            }

            commit()
            return result
        } catch (ex: Throwable) {
            rollback()
            throw ex
        } finally {
            transaction.complete()
        }
    }
}

/**
 * Disables [autoCommit][Connection.getAutoCommit] mode on `this` [Connection], then calls a specific function [block]
 * with `this` [Connection] as its receiver and returns its result, then sets the [autoCommit][Connection.getAutoCommit]
 * mode on `this` [Connection] back to its original value.
 */
@PublishedApi
internal inline fun <T> Connection.runWithManualCommit(block: Connection.() -> T): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    val before = autoCommit

    return try {
        autoCommit = false
        this.run(block)
    } finally {
        autoCommit = before
    }
}
