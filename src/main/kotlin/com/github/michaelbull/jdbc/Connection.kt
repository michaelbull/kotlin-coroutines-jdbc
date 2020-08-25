package com.github.michaelbull.jdbc

import com.github.michaelbull.jdbc.context.CoroutineConnection
import com.github.michaelbull.jdbc.context.dataSource
import com.github.michaelbull.logging.InlineLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.SQLException
import javax.sql.DataSource
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

@PublishedApi
internal val logger = InlineLogger()

/**
 * Calls the specified suspending [block] in the context of a [CoroutineConnection], suspends until it completes, and
 * returns the result.
 *
 * When the [coroutineContext] has an [open connection][hasOpenConnection] the [block] will be immediately invoked
 * within that context.
 *
 * When the [coroutineContext] has no [Connection], or it [is closed][isClosedCatching], the [block] will be invoked in
 * the context of a new [Connection]. The new [Connection] will be created by the [DataSource] in the
 * [coroutineContext], throwing an [IllegalStateException] if no such [DataSource] exists. After the [block] is invoked,
 * the newly established [Connection] will be [closed][closeCatching].
 */
suspend inline fun <T> withConnection(crossinline block: suspend CoroutineScope.() -> T): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    return if (coroutineContext.hasOpenConnection()) {
        withContext(coroutineContext) {
            block()
        }
    } else {
        val newConnection = coroutineContext.dataSource.connection

        withContext(CoroutineConnection(newConnection)) {
            try {
                block()
            } finally {
                newConnection.closeCatching()
            }
        }
    }
}

/**
 * Returns `true` if this [CoroutineContext] container a [Connection] that is not [closed][isClosedCatching],
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
