package com.github.michaelbull.jdbc

import com.github.michaelbull.jdbc.context.CoroutineConnection
import com.github.michaelbull.jdbc.context.dataSource
import com.github.michaelbull.logging.InlineLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.SQLException
import javax.sql.DataSource
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.CoroutineContext

@PublishedApi
internal val logger = InlineLogger()

/**
 * Calls the specified suspending [block] [with the context][withContext] of a [CoroutineConnection], suspends until it
 * completes, and returns the result.
 *
 * When the [currentCoroutineContext] has an [open][hasOpenConnection] [Connection], the specified suspending [block]
 * will be called [with this context][withContext].
 *
 * When the [currentCoroutineContext] has no [Connection], or it [is closed][isClosedCatching], the specified suspending
 * [block] will be called [with the context][withContext] of a new [Connection]. This new [Connection] will be
 * established from the [DataSource] in the [currentCoroutineContext], or throw an [IllegalStateException] if no such
 * [DataSource] exists, and will be [closed][closeCatching] after the specified suspending [block] completes.
 */
suspend inline fun <T> withConnection(crossinline block: suspend CoroutineScope.() -> T): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    val ctx = currentCoroutineContext()

    return if (ctx.hasOpenConnection()) {
        withContext(currentCoroutineContext()) {
            block()
        }
    } else {
        val connection = ctx.dataSource.connection

        try {
            withContext(CoroutineConnection(connection)) {
                block()
            }
        } finally {
            connection.closeCatching()
        }
    }
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
