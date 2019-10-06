package com.github.michaelbull.jdbc

import com.github.michaelbull.jdbc.context.CoroutineConnection
import com.github.michaelbull.jdbc.context.dataSource
import com.github.michaelbull.logging.InlineLogger
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.SQLException
import kotlin.coroutines.coroutineContext

private val logger = InlineLogger()

suspend fun <T> withConnection(block: suspend () -> T): T {
    val connection = coroutineContext[CoroutineConnection]

    return if (connection.isNullOrClosed()) {
        newConnection(block)
    } else {
        block()
    }
}

private suspend fun <T> newConnection(block: suspend () -> T): T {
    val connection = coroutineContext.dataSource?.connection ?: error("No data source in context")

    return try {
        withContext(CoroutineConnection(connection)) {
            block()
        }
    } finally {
        connection.closeCatching()
    }
}

@Suppress("NOTHING_TO_INLINE")
private inline fun Connection?.isNullOrClosed(): Boolean {
    return if (this == null) {
        true
    } else try {
        isClosed
    } catch (ex: SQLException) {
        logger.warn(ex) { "Connection isNullOrClosed check failed, assuming closed:" }
        true
    }
}

@PublishedApi
internal fun Connection.rollbackCatching() {
    try {
        rollback()
    } catch (ex: Throwable) {
        logger.warn(ex) { "Failed to rollback transaction:" }
    }
}

private fun Connection.closeCatching() {
    try {
        close()
    } catch (ex: SQLException) {
        logger.warn(ex) { "Failed to close database connection cleanly:" }
    }
}
