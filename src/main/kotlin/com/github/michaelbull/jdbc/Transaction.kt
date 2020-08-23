package com.github.michaelbull.jdbc

import com.github.michaelbull.jdbc.context.CoroutineConnection
import com.github.michaelbull.jdbc.context.CoroutineTransaction
import com.github.michaelbull.jdbc.context.connection
import com.github.michaelbull.jdbc.context.transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import java.sql.Connection
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

/**
 * Calls the specified suspending [block] in the context of a [CoroutineTransaction], suspends until it completes, and
 * returns the result.
 *
 * When there exists a [CoroutineTransaction] in the current [CoroutineContext], the [block] will be immediately invoked
 * if the [transaction is running][CoroutineTransaction.isRunning], otherwise an [IllegalStateException] will be thrown.
 *
 * When no [CoroutineTransaction] exists in the current [CoroutineContext], the [block] will be invoked
 * [with the context][withContext] of a new [CoroutineTransaction].
 *
 * The [block] will be invoked [with a connection][withConnection] in its [CoroutineContext]. The connection's
 * [autoCommit][Connection.setAutoCommit] is set to `false` before the invocation. If the [block] throws a [Throwable],
 * the transaction will [rollback][Connection.rollback] and re-throw the [Throwable], otherwise the transaction will
 * [commit][Connection.commit] and return the result of type [T].
 */
suspend inline fun <T> transaction(crossinline block: suspend CoroutineScope.() -> T): T {
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
        existingTransaction.isRunning -> block(CoroutineScope(coroutineContext))
        else -> error("Attempted to start new transaction within: $existingTransaction")
    }
}

/**
 * [Starts][CoroutineTransaction.start] the current [CoroutineTransaction] and sets the
 * current [CoroutineConnection]'s [autoCommit][Connection.setAutoCommit] to `false`, calls the specified suspending
 * [block], suspends until it completes, then [commits][Connection.commit] and returns the result.
 *
 * If the [block] throws a [Throwable], the connection will [rollback][Connection.rollback] and not
 * [commit][Connection.commit].
 */
@PublishedApi
internal suspend inline fun <T> execute(crossinline block: suspend CoroutineScope.() -> T): T {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }

    val transaction = coroutineContext.transaction ?: error("No transaction in context")
    transaction.start()

    val connection = coroutineContext.connection
    connection.autoCommit = false

    try {
        val result = block(CoroutineScope(coroutineContext))
        transaction.complete()
        connection.commit()
        return result
    } catch (ex: Throwable) {
        transaction.complete()
        connection.rollback()
        throw ex
    }
}
