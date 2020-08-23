package com.github.michaelbull.jdbc

import com.github.michaelbull.jdbc.context.CoroutineConnection
import com.github.michaelbull.jdbc.context.CoroutineDataSource
import com.github.michaelbull.jdbc.context.dataSource
import com.github.michaelbull.logging.InlineLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.SQLException
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
 * When there exists a [CoroutineConnection] in the current [CoroutineContext], the [block] will be immediately invoked
 * if the [connection is not closed][Connection.isClosed].
 *
 * When there exists no [CoroutineConnection] in the current [CoroutineContext], or when the [CoroutineConnection] in
 * the current [CoroutineContext] is [closed][Connection.isClosed], the [block] will be invoked
 * [with the context][withContext] of a new [CoroutineConnection] and an attempt will be made to [Connection.close]
 * it afterwards.
 */
suspend inline fun <T> withConnection(crossinline block: suspend CoroutineScope.() -> T): T {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }

    val connection = coroutineContext[CoroutineConnection]

    return if (connection.isNullOrClosed()) {
        newConnection(block)
    } else {
        block(CoroutineScope(coroutineContext))
    }
}

/**
 * Calls the specified suspending [block] with the context of a new [CoroutineConnection], suspends until it completes,
 * attempts to [close][closeCatching] the [CoroutineConnection], and returns the result.
 *
 * If no [CoroutineDataSource] exists in the current [CoroutineContext] from which a [Connection] can be attained, an
 * [IllegalStateException] is thrown.
 */
@PublishedApi
internal suspend inline fun <T> newConnection(crossinline block: suspend CoroutineScope.() -> T): T {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }

    val connection = coroutineContext.dataSource?.connection ?: error("No data source in context")

    return try {
        withContext(CoroutineConnection(connection)) {
            block()
        }
    } finally {
        connection.closeCatching()
    }
}

/**
 * Returns `true` if this nullable [Connection] is either `null` or [isClosed][Connection.isClosed], catching any
 * [Throwable] exception that was thrown from the call to [Connection.isClosed] and assuming it to be `true`.
 */
@PublishedApi
internal fun Connection?.isNullOrClosed(): Boolean {
    contract {
        returns(false) implies (this@isNullOrClosed != null)
    }

    return if (this == null) {
        true
    } else try {
        isClosed
    } catch (ex: SQLException) {
        logger.warn(ex) { "Connection isNullOrClosed check failed, assuming closed:" }
        true
    }
}

/**
 * Calls [Connection.close] on this [Connection], catching any [Throwable] exception that was thrown from the call to
 * [Connection.close] and logging it.
 */
@PublishedApi
internal fun Connection.closeCatching() {
    try {
        close()
    } catch (ex: SQLException) {
        logger.warn(ex) { "Failed to close database connection cleanly:" }
    }
}
