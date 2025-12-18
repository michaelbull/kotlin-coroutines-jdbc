package com.github.michaelbull.jdbc

import com.github.michaelbull.jdbc.context.CoroutineConnection
import com.github.michaelbull.jdbc.context.CoroutineTransaction
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.Connection.TRANSACTION_READ_COMMITTED
import java.sql.Connection.TRANSACTION_SERIALIZABLE
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class TransactionTest {

    @Test
    fun `transaction throws IllegalStateException if no connection in context`() = runTest {
        assertFailsWith<IllegalStateException> {
            transaction {
                /* empty */
            }
        }
    }

    @Test
    fun `transaction throws IllegalStateException if existing transaction in context`() = runTest {
        assertFailsWith<IllegalStateException> {
            withContext(CoroutineTransaction) {
                transaction {
                    /* empty */
                }
            }
        }
    }

    @Test
    fun `transaction throws IllegalStateException if nested`() = runTest {
        val connection = mockk<Connection>(relaxed = true) {
            every { isClosed } returns false
            every { autoCommit } returns true
        }

        assertFailsWith<IllegalStateException> {
            withContext(CoroutineConnection(connection)) {
                transaction {
                    transaction {
                        /* empty */
                    }
                }
            }
        }
    }

    @Test
    fun `transaction adds CoroutineTransaction to context`() = runTest {
        val connection = mockk<Connection>(relaxed = true) {
            every { isClosed } returns false
            every { autoCommit } returns true
        }

        val actual = withContext(CoroutineConnection(connection)) {
            transaction {
                currentCoroutineContext()[CoroutineTransaction]
            }
        }

        assertNotNull(actual)
    }

    @Test
    fun `transaction calls commit on success`() = runTest {
        val connection = mockk<Connection>(relaxed = true) {
            every { isClosed } returns false
            every { autoCommit } returns true
        }

        withContext(CoroutineConnection(connection)) {
            transaction {
                /* empty */
            }
        }

        verify(exactly = 1) { connection.commit() }
    }

    @Test
    fun `transaction calls rollback on failure`() = runTest {
        val connection = mockk<Connection>(relaxed = true) {
            every { isClosed } returns false
            every { autoCommit } returns true
        }

        try {
            withContext(CoroutineConnection(connection)) {
                transaction {
                    throw IllegalArgumentException()
                }
            }
        } catch (_: IllegalArgumentException) {
            /* empty */
        }

        verify(exactly = 1) { connection.rollback() }
    }

    @Test
    fun `transaction rethrows exceptions`() = runTest {
        val connection = mockk<Connection>(relaxed = true) {
            every { isClosed } returns false
            every { autoCommit } returns true
        }

        assertFailsWith<IllegalArgumentException> {
            withContext(CoroutineConnection(connection)) {
                transaction {
                    throw IllegalArgumentException()
                }
            }
        }
    }

    @Test
    fun `isolatedTransaction throws IllegalStateException if no connection in context`() = runTest {
        assertFailsWith<IllegalStateException> {
            isolatedTransaction(TRANSACTION_SERIALIZABLE) {
                /* empty */
            }
        }
    }

    @Test
    fun `isolatedTransaction throws IllegalStateException if existing transaction in context`() = runTest {
        assertFailsWith<IllegalStateException> {
            withContext(CoroutineTransaction) {
                isolatedTransaction(TRANSACTION_SERIALIZABLE) {
                    /* empty */
                }
            }
        }
    }

    @Test
    fun `isolatedTransaction sets transaction isolation level`() = runTest {
        val connection = mockk<Connection>(relaxed = true) {
            every { isClosed } returns false
            every { autoCommit } returns true
            every { transactionIsolation } returns TRANSACTION_READ_COMMITTED
        }

        withContext(CoroutineConnection(connection)) {
            isolatedTransaction(TRANSACTION_SERIALIZABLE) {
                /* empty */
            }
        }

        verify { connection.transactionIsolation = TRANSACTION_SERIALIZABLE }
    }

    @Test
    fun `isolatedTransaction calls commit on success`() = runTest {
        val connection = mockk<Connection>(relaxed = true) {
            every { isClosed } returns false
            every { autoCommit } returns true
            every { transactionIsolation } returns TRANSACTION_READ_COMMITTED
        }

        withContext(CoroutineConnection(connection)) {
            isolatedTransaction(TRANSACTION_SERIALIZABLE) {
                /* empty */
            }
        }

        verify(exactly = 1) { connection.commit() }
    }

    @Test
    fun `isolatedTransaction calls rollback on failure`() = runTest {
        val connection = mockk<Connection>(relaxed = true) {
            every { isClosed } returns false
            every { autoCommit } returns true
            every { transactionIsolation } returns TRANSACTION_READ_COMMITTED
        }

        try {
            withContext(CoroutineConnection(connection)) {
                isolatedTransaction(TRANSACTION_SERIALIZABLE) {
                    throw IllegalArgumentException()
                }
            }
        } catch (_: IllegalArgumentException) {
            /* empty */
        }

        verify(exactly = 1) { connection.rollback() }
    }

    @Test
    fun `readOnlyTransaction throws IllegalStateException if no connection in context`() = runTest {
        assertFailsWith<IllegalStateException> {
            readOnlyTransaction {
                /* empty */
            }
        }
    }

    @Test
    fun `readOnlyTransaction throws IllegalStateException if existing transaction in context`() = runTest {
        assertFailsWith<IllegalStateException> {
            withContext(CoroutineTransaction) {
                readOnlyTransaction {
                    /* empty */
                }
            }
        }
    }

    @Test
    fun `readOnlyTransaction sets read-only mode`() = runTest {
        val connection = mockk<Connection>(relaxed = true) {
            every { isClosed } returns false
            every { autoCommit } returns true
            every { isReadOnly } returns false
        }

        withContext(CoroutineConnection(connection)) {
            readOnlyTransaction {
                /* empty */
            }
        }

        verify { connection.isReadOnly = true }
    }

    @Test
    fun `readOnlyTransaction calls commit on success`() = runTest {
        val connection = mockk<Connection>(relaxed = true) {
            every { isClosed } returns false
            every { autoCommit } returns true
            every { isReadOnly } returns false
        }

        withContext(CoroutineConnection(connection)) {
            readOnlyTransaction {
                /* empty */
            }
        }

        verify(exactly = 1) { connection.commit() }
    }

    @Test
    fun `readOnlyTransaction calls rollback on failure`() = runTest {
        val connection = mockk<Connection>(relaxed = true) {
            every { isClosed } returns false
            every { autoCommit } returns true
            every { isReadOnly } returns false
        }

        try {
            withContext(CoroutineConnection(connection)) {
                readOnlyTransaction {
                    throw IllegalArgumentException()
                }
            }
        } catch (_: IllegalArgumentException) {
            /* empty */
        }

        verify(exactly = 1) { connection.rollback() }
    }

    @Test
    fun `isolatedReadOnlyTransaction throws IllegalStateException if no connection in context`() = runTest {
        assertFailsWith<IllegalStateException> {
            isolatedReadOnlyTransaction(TRANSACTION_SERIALIZABLE) {
                /* empty */
            }
        }
    }

    @Test
    fun `isolatedReadOnlyTransaction throws IllegalStateException if existing transaction in context`() = runTest {
        assertFailsWith<IllegalStateException> {
            withContext(CoroutineTransaction) {
                isolatedReadOnlyTransaction(TRANSACTION_SERIALIZABLE) {
                    /* empty */
                }
            }
        }
    }

    @Test
    fun `isolatedReadOnlyTransaction sets transaction isolation level and read-only mode`() = runTest {
        val connection = mockk<Connection>(relaxed = true) {
            every { isClosed } returns false
            every { autoCommit } returns true
            every { transactionIsolation } returns TRANSACTION_READ_COMMITTED
            every { isReadOnly } returns false
        }

        withContext(CoroutineConnection(connection)) {
            isolatedReadOnlyTransaction(TRANSACTION_SERIALIZABLE) {
                /* empty */
            }
        }

        verify { connection.transactionIsolation = TRANSACTION_SERIALIZABLE }
        verify { connection.isReadOnly = true }
    }

    @Test
    fun `isolatedReadOnlyTransaction calls commit on success`() = runTest {
        val connection = mockk<Connection>(relaxed = true) {
            every { isClosed } returns false
            every { autoCommit } returns true
            every { transactionIsolation } returns TRANSACTION_READ_COMMITTED
            every { isReadOnly } returns false
        }

        withContext(CoroutineConnection(connection)) {
            isolatedReadOnlyTransaction(TRANSACTION_SERIALIZABLE) {
                /* empty */
            }
        }

        verify(exactly = 1) { connection.commit() }
    }

    @Test
    fun `isolatedReadOnlyTransaction calls rollback on failure`() = runTest {
        val connection = mockk<Connection>(relaxed = true) {
            every { isClosed } returns false
            every { autoCommit } returns true
            every { transactionIsolation } returns TRANSACTION_READ_COMMITTED
            every { isReadOnly } returns false
        }

        try {
            withContext(CoroutineConnection(connection)) {
                isolatedReadOnlyTransaction(TRANSACTION_SERIALIZABLE) {
                    throw IllegalArgumentException()
                }
            }
        } catch (_: IllegalArgumentException) {
            /* empty */
        }

        verify(exactly = 1) { connection.rollback() }
    }
}
