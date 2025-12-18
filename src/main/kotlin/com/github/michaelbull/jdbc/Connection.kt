package com.github.michaelbull.jdbc

import com.github.michaelbull.jdbc.context.CoroutineConnection
import com.github.michaelbull.jdbc.context.connection
import com.github.michaelbull.jdbc.context.dataSource
import com.github.michaelbull.logging.InlineLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.SQLException
import javax.sql.DataSource
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.CoroutineContext

@PublishedApi
internal val logger: InlineLogger = InlineLogger()

/**
 * Calls the specified suspending [block] with a [CoroutineConnection] in scope, suspends until it completes, and
 * returns the result.
 *
 * When the [currentCoroutineContext] has an [open][hasOpenConnection] [Connection], the specified suspending [block]
 * will be called within a new [coroutineScope].
 *
 * When the [currentCoroutineContext] has no [Connection], or it [is closed][isClosedCatching], the specified suspending
 * [block] will be called [with the context][withContext] of a new [Connection]. This new [Connection] will be
 * established from the [DataSource] in the [currentCoroutineContext], or throw an [IllegalStateException] if no such
 * [DataSource] exists, and will be [closed][closeCatching] after the specified suspending [block] completes.
 */
public suspend inline fun <T> withConnection(crossinline block: suspend CoroutineScope.(connection: Connection) -> T): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    val ctx = currentCoroutineContext()
    val existingConnection = ctx[CoroutineConnection]?.connection

    return if (existingConnection == null || existingConnection.isClosedCatching()) {
        val newConnection = ctx.dataSource.connection

        try {
            withContext(CoroutineConnection(newConnection)) {
                block(newConnection)
            }
        } finally {
            newConnection.closeCatching()
        }
    } else {
        coroutineScope {
            block(existingConnection)
        }
    }
}

/**
 * Disables [autoCommit][Connection.getAutoCommit] mode on this [Connection], calls the specified [block] and returns
 * its result, then restores [autoCommit][Connection.getAutoCommit] to its original value.
 */
@PublishedApi
internal inline fun <T> Connection.withManualCommit(block: () -> T): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    val before = autoCommit

    return try {
        autoCommit = false
        block()
    } finally {
        autoCommit = before
    }
}

/**
 * Enables [readOnly][Connection.setReadOnly] mode on this [Connection], calls the specified [block] and returns its
 * result, then restores [readOnly][Connection.isReadOnly] to its original value.
 */
@PublishedApi
internal inline fun <T> Connection.withReadOnly(block: () -> T): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    val before = isReadOnly

    return try {
        isReadOnly = true
        block()
    } finally {
        isReadOnly = before
    }
}

/**
 * Sets [transactionIsolation][Connection.setTransactionIsolation] to the specified [isolationLevel] on this [Connection],
 * calls the specified [block] and returns its result, then restores
 * [transactionIsolation][Connection.getTransactionIsolation] to its original value.
 */
@PublishedApi
internal inline fun <T> Connection.withIsolation(isolationLevel: Int, block: () -> T): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    val before = transactionIsolation

    return try {
        transactionIsolation = isolationLevel
        block()
    } finally {
        transactionIsolation = before
    }
}

/**
 * Returns the [Connection] in the [currentCoroutineContext], or throws an [IllegalStateException] if no such
 * [Connection] exists.
 */
public suspend fun currentConnection(): Connection {
    return currentCoroutineContext().connection
}

/**
 * Returns `true` if this [CoroutineContext] contains a [Connection] that is not [closed][isClosedCatching],
 * otherwise `false`.
 */
@PublishedApi
internal fun CoroutineContext.hasOpenConnection(): Boolean {
    val connection = get(CoroutineConnection)?.connection
    return connection != null && !connection.isClosedCatching()
}

/**
 * Calls [close][Connection.close] on this [Connection], catching any [SQLException] that was thrown and logging it.
 */
@PublishedApi
internal fun Connection.closeCatching() {
    try {
        close()
    } catch (ex: SQLException) {
        logger.warn(ex) { "Failed to close database connection cleanly:" }
    }
}

/**
 * Calls [isClosed][Connection.isClosed] on this [Connection] and returns its result, catching any [SQLException] that
 * was thrown then logging it and returning `true`.
 */
@PublishedApi
internal fun Connection.isClosedCatching(): Boolean {
    return try {
        isClosed
    } catch (ex: SQLException) {
        logger.warn(ex) { "Connection isClosedCatching check failed, assuming closed:" }
        true
    }
}
