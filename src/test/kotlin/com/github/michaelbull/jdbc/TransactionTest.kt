package com.github.michaelbull.jdbc

import com.github.michaelbull.jdbc.context.CoroutineConnection
import com.github.michaelbull.jdbc.context.CoroutineTransaction
import com.github.michaelbull.jdbc.context.transaction
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import java.sql.Connection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

@ExperimentalCoroutinesApi
class TransactionTest {

    @Test
    fun `nested transactions`() = runTest {
        val connection = mockk<Connection>(relaxed = true) {
            every { isClosed } returns false
            every { autoCommit } returns true
        }

        val expected = (((((5 * 2) * 3) / 2) + 5) * 100) / 2
        var actual = 0

        withContext(CoroutineConnection(connection)) {
            transaction {
                actual = 5

                transaction {
                    actual *= 2
                }

                transaction {
                    actual *= 3
                }

                transaction {
                    actual /= 2

                    transaction {
                        actual += 5

                        transaction {
                            actual *= 100
                        }
                    }
                }

                transaction {
                    actual /= 2
                }
            }
        }

        assertEquals(expected, actual)
    }

    @Test
    fun `transaction reuses existing transaction in context if incomplete`() = runTest {
        val incompleteTransaction = CoroutineTransaction(completed = false)

        val actual = withContext(incompleteTransaction) {
            transaction {
                coroutineContext.transaction
            }
        }

        assertEquals(incompleteTransaction, actual)
    }

    @Test
    fun `transaction throws IllegalStateException if existing transaction in context is completed`() = runTest {
        val completeTransaction = CoroutineTransaction(completed = true)

        assertFailsWith<IllegalStateException> {
            withContext(completeTransaction) {
                transaction {
                    /* empty */
                }
            }
        }
    }

    @Test
    fun `transaction throws IllegalStateException if context is empty`() = runTest {
        assertFailsWith<IllegalStateException> {
            transaction {
                /* empty */
            }
        }
    }

    @Test
    fun `transaction adds new transaction to context if no transaction in context`() = runTest {
        val connection = mockk<Connection>(relaxed = true) {
            every { isClosed } returns false
            every { autoCommit } returns true
        }

        val actual = withContext(CoroutineConnection(connection)) {
            transaction {
                coroutineContext.transaction
            }
        }

        assertNotNull(actual)
    }

    @Test
    fun `runTransactionally adds new transaction to context`() = runTest {
        val connection = mockk<Connection>(relaxed = true) {
            every { autoCommit } returns true
        }

        val actual = withContext(CoroutineConnection(connection)) {
            runTransactionally {
                coroutineContext.transaction
            }
        }

        assertNotNull(actual)
    }

    @Test
    fun `runTransactionally calls commit on success`() = runTest {
        val connection = mockk<Connection>(relaxed = true) {
            every { autoCommit } returns true
        }

        withContext(CoroutineConnection(connection)) {
            runTransactionally {
                /* empty */
            }
        }

        verify(exactly = 1) { connection.commit() }
    }

    @Test
    fun `runTransactionally does not call rollback on success`() = runTest {
        val connection = mockk<Connection>(relaxed = true) {
            every { autoCommit } returns true
        }

        withContext(CoroutineConnection(connection)) {
            runTransactionally {
                /* empty */
            }
        }

        verify(exactly = 0) { connection.rollback() }
    }

    @Test
    fun `runTransactionally calls rollback on failure`() = runTest {
        val connection = mockk<Connection>(relaxed = true) {
            every { autoCommit } returns true
        }

        try {
            withContext(CoroutineConnection(connection)) {
                runTransactionally {
                    throw Throwable()
                }
            }
        } catch (_: Throwable) {
            /* empty */
        }

        verify(exactly = 1) { connection.rollback() }
    }

    @Test
    fun `runTransactionally does not call commit on failure`() = runTest {
        val connection = mockk<Connection>(relaxed = true) {
            every { autoCommit } returns true
        }

        try {
            withContext(CoroutineConnection(connection)) {
                runTransactionally {
                    throw Throwable()
                }
            }
        } catch (_: Throwable) {
            /* empty */
        }

        verify(exactly = 0) { connection.commit() }
    }

    @Test
    fun `runTransactionally rethrows exceptions`() = runTest {
        val connection = mockk<Connection>(relaxed = true) {
            every { autoCommit } returns true
        }

        assertFailsWith<IllegalArgumentException> {
            withContext(CoroutineConnection(connection)) {
                runTransactionally {
                    throw IllegalArgumentException()
                }
            }
        }
    }

    @Test
    fun `runWithManualCommit sets autoCommit to false before running`() {
        val connection = mockk<Connection> {
            every { autoCommit } returns true
            every { autoCommit = any() } just runs
        }

        connection.runWithManualCommit {
            verify(exactly = 1) { connection.autoCommit = false }
        }
    }

    @Test
    fun `runWithManualCommit restores autoCommit to original value after running`() {
        val connection = mockk<Connection> {
            every { autoCommit } returns true
            every { autoCommit = any() } just runs
        }

        connection.runWithManualCommit {
            /* empty */
        }

        verify(exactly = 1) { connection.autoCommit = true }
    }
}
