package com.github.michaelbull.jdbc

import com.github.michaelbull.jdbc.context.CoroutineConnection
import com.github.michaelbull.jdbc.context.CoroutineTransaction
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.sql.Connection

@ExperimentalCoroutinesApi
class TransactionTest {

    @Test
    fun `nested transactions`() {
        val connection = mockk<Connection>(relaxed = true) {
            every { isClosed } returns false
            every { autoCommit } returns true
        }

        val context = CoroutineConnection(connection)

        val expected = (((((5 * 2) * 3) / 2) + 5) * 100) / 2
        var actual = 0

        runBlockingTest(context) {
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
    fun `transaction reuses existing transaction in context if incomplete`() {
        val incompleteTransaction = CoroutineTransaction(completed = false)

        runBlockingTest(incompleteTransaction) {
            val actual = transaction {
                coroutineContext[CoroutineTransaction]
            }

            assertEquals(incompleteTransaction, actual)
        }
    }

    @Test
    fun `transaction throws IllegalStateException if existing transaction in context is completed`() {
        val completeTransaction = CoroutineTransaction(completed = true)

        assertThrows<IllegalStateException> {
            runBlockingTest(completeTransaction) {
                transaction { }
            }
        }
    }

    @Test
    fun `transaction throws IllegalStateException if context is empty`() {
        assertThrows<IllegalStateException> {
            runBlockingTest {
                transaction {

                }
            }
        }
    }

    @Test
    fun `transaction adds new transaction to context if no transaction in context`() {
        val connection = mockk<Connection>(relaxed = true) {
            every { isClosed } returns false
            every { autoCommit } returns true
        }

        val context = CoroutineConnection(connection)

        runBlockingTest(context) {
            val transaction = transaction {
                coroutineContext[CoroutineTransaction]
            }

            assertNotNull(transaction)
        }
    }

    @Test
    fun `runTransactionally adds new transaction to context`() {
        val connection = mockk<Connection>(relaxed = true) {
            every { autoCommit } returns true
        }

        val context = CoroutineConnection(connection)

        runBlockingTest(context) {
            val transaction = runTransactionally {
                coroutineContext[CoroutineTransaction]
            }

            assertNotNull(transaction)
        }
    }

    @Test
    fun `runTransactionally calls commit on success`() {
        val connection = mockk<Connection>(relaxed = true) {
            every { autoCommit } returns true
        }

        val context = CoroutineConnection(connection)

        runBlockingTest(context) {
            runTransactionally {}
        }

        verify(exactly = 1) { connection.commit() }
    }

    @Test
    fun `runTransactionally does not call rollback on success`() {
        val connection = mockk<Connection>(relaxed = true) {
            every { autoCommit } returns true
        }

        val context = CoroutineConnection(connection)

        runBlockingTest(context) {
            runTransactionally {}
        }

        verify(exactly = 0) { connection.rollback() }
    }

    @Test
    fun `runTransactionally calls rollback on failure`() {
        val connection = mockk<Connection>(relaxed = true) {
            every { autoCommit } returns true
        }

        val context = CoroutineConnection(connection)

        try {
            runBlockingTest(context) {
                runTransactionally {
                    throw Throwable()
                }
            }
        } catch (ignored: Throwable) {

        }

        verify(exactly = 1) { connection.rollback() }
    }

    @Test
    fun `runTransactionally does not call commit on failure`() {
        val connection = mockk<Connection>(relaxed = true) {
            every { autoCommit } returns true
        }

        val context = CoroutineConnection(connection)

        try {
            runBlockingTest(context) {
                runTransactionally {
                    throw Throwable()
                }
            }
        } catch (ignored: Throwable) {

        }

        verify(exactly = 0) { connection.commit() }
    }

    @Test
    fun `runTransactionally rethrows exceptions`() {
        val connection = mockk<Connection>(relaxed = true) {
            every { autoCommit } returns true
        }

        val context = CoroutineConnection(connection)

        assertThrows<IllegalArgumentException> {
            runBlockingTest(context) {
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

        connection.runWithManualCommit { }

        verify(exactly = 1) { connection.autoCommit = true }
    }
}
