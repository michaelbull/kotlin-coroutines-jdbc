package com.github.michaelbull.jdbc

import com.github.michaelbull.jdbc.context.CoroutineTransaction
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import java.sql.Connection
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Calls the specified suspending [block] in the context of a [CoroutineTransaction], suspends until it completes, and
 * returns the result.
 *
 * Throws an [IllegalStateException] if the [currentCoroutineContext] already has a [CoroutineTransaction], as
 * transactions cannot be nested.
 */
public suspend inline fun <T> transaction(crossinline block: suspend () -> T): T {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }

    checkOutsideTransaction()

    return withConnection {
        withManualCommit {
            commitOrRollback(block)
        }
    }
}

/**
 * Calls the specified suspending [block] in the context of a [CoroutineTransaction] with the specified
 * [isolation][Connection.setTransactionIsolation] level, suspends until it completes, and returns the result.
 *
 * Throws an [IllegalStateException] if the [currentCoroutineContext] already has a [CoroutineTransaction], as
 * transactions cannot be nested.
 */
public suspend inline fun <T> isolatedTransaction(
    isolationLevel: Int,
    crossinline block: suspend () -> T,
): T {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }

    checkOutsideTransaction()

    return withConnection {
        withIsolation(isolationLevel) {
            withManualCommit {
                commitOrRollback(block)
            }
        }
    }
}

/**
 * Calls the specified suspending [block] in the context of a [read-only][Connection.setReadOnly]
 * [CoroutineTransaction], suspends until it completes, and returns the result.
 *
 * Throws an [IllegalStateException] if the [currentCoroutineContext] already has a [CoroutineTransaction], as
 * transactions cannot be nested.
 */
public suspend inline fun <T> readOnlyTransaction(crossinline block: suspend () -> T): T {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }

    checkOutsideTransaction()

    return withConnection {
        withReadOnly {
            withManualCommit {
                commitOrRollback(block)
            }
        }
    }
}

/**
 * Calls the specified suspending [block] in the context of a [read-only][Connection.setReadOnly]
 * [CoroutineTransaction] with the specified [isolation][Connection.setTransactionIsolation] level, suspends until it
 * completes, and returns the result.
 *
 * Throws an [IllegalStateException] if the [currentCoroutineContext] already has a [CoroutineTransaction], as
 * transactions cannot be nested.
 */
public suspend inline fun <T> isolatedReadOnlyTransaction(
    isolationLevel: Int,
    crossinline block: suspend () -> T,
): T {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }

    checkOutsideTransaction()

    return withConnection {
        withIsolation(isolationLevel) {
            withReadOnly {
                withManualCommit {
                    commitOrRollback(block)
                }
            }
        }
    }
}

/**
 * Throws an [IllegalStateException] if the [currentCoroutineContext] has a [CoroutineTransaction].
 */
@PublishedApi
internal suspend fun checkOutsideTransaction() {
    val ctx = currentCoroutineContext()
    val transaction = ctx[CoroutineTransaction]

    check(transaction == null) {
        "Transactions cannot be nested"
    }
}

/**
 * Calls the specified suspending [block] with [CoroutineTransaction] in the [coroutineContext][withContext], then
 * calls [commit][Connection.commit] on success or [rollback][Connection.rollback] on failure.
 */
@PublishedApi
internal suspend inline fun <T> Connection.commitOrRollback(crossinline block: suspend () -> T): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    try {
        val result = withContext(CoroutineTransaction) {
            block()
        }

        withContext(NonCancellable) {
            commit()
        }

        return result
    } catch (exception: Throwable) {
        withContext(NonCancellable) {
            try {
                rollback()
            } catch (rollbackException: Throwable) {
                exception.addSuppressed(rollbackException)
            }
        }

        throw exception
    }
}
