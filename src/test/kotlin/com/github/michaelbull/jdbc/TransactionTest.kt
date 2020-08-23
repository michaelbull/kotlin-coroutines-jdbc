package com.github.michaelbull.jdbc

import com.github.michaelbull.jdbc.context.CoroutineConnection
import com.github.michaelbull.jdbc.context.CoroutineDataSource
import com.github.michaelbull.jdbc.context.CoroutineTransaction
import com.github.michaelbull.jdbc.context.transaction
import com.github.michaelbull.jdbc.context.connection
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.sql.Connection
import javax.sql.DataSource
import kotlin.coroutines.coroutineContext

@ExperimentalCoroutinesApi
class TransactionTest {

    private val openConnection = mockk<Connection>("OpenConnection", relaxed = true).apply {
        every { isClosed } returns false
    }

    private val dataSource = mockk<DataSource>(relaxed = true).apply {
        every { connection } returns openConnection
    }

    @Test
    fun `no longer fails to get connection inside transaction block`() {
        val context = CoroutineDataSource(dataSource)

        runBlockingTest(context) {
            transaction {
                // the code inside the transaction would use the CoroutineContext
                // that was created before calling the transaction function.

                // This happened because `this` referred to the parent CoroutineScope.

                // this will fail unless transaction returns its own scope
                // the scope of runBlockingTest was used before, which didn't have connection or transaction in context
                coroutineContext.connection
            }
        }
    }

    @Test
    fun `transaction should throw exception if no data source in context`() {
        assertThrows<IllegalStateException> {
            runBlockingTest {
                transaction { }
            }
        }
    }

    @Test
    fun `transaction should add new transaction to context if absent`() {
        runBlockingTest(CoroutineDataSource(dataSource)) {
            transaction {
                assertTransactionNotNull()
            }
        }
    }

    @Test
    fun `transaction should throw exception if existing transaction inactive`() {
        assertThrows<IllegalStateException> {
            runBlockingTest(CoroutineTransaction()) {
                transaction { }
            }
        }
    }

    @Test
    fun `transaction should throw exception if existing transaction completed`() {
        val transaction = CoroutineTransaction()
        transaction.start()
        transaction.complete()

        assertThrows<IllegalStateException> {
            runBlockingTest(transaction) {
                transaction { }
            }
        }
    }

    @Test
    fun `transaction should reuse existing transaction if still running`() {
        val transaction = CoroutineTransaction()
        val context = transaction + CoroutineDataSource(dataSource) + CoroutineConnection(openConnection)

        transaction.start()

        runBlockingTest(context) {
            transaction {
                assertTransactionEquals(transaction)
            }
        }
    }

    @Test
    fun `transaction should disable connection autocommit in new transaction`() {
        val context = CoroutineDataSource(dataSource)

        runBlockingTest(context) {
            transaction {
                verify(exactly = 1) { openConnection.autoCommit = false }
            }
        }
    }

    @Test
    fun `transaction should not disable autocommit when reusing transaction`() {
        val transaction = CoroutineTransaction()
        val context = transaction + CoroutineDataSource(dataSource) + CoroutineConnection(openConnection)

        transaction.start()

        runBlockingTest(context) {
            transaction {
                verify(exactly = 0) { openConnection.autoCommit = false }
            }
        }
    }

    @Test
    fun `transaction should commit a successful transaction`() {
        val context = CoroutineDataSource(dataSource)

        runBlockingTest(context) {
            transaction { }
        }

        verify(exactly = 1) { openConnection.commit() }
    }

    @Test
    fun `transaction should rollback an unsuccessful transaction`() {
        val context = CoroutineDataSource(dataSource)

        try {
            runBlockingTest(context) {
                @Suppress("IMPLICIT_NOTHING_AS_TYPE_PARAMETER")
                transaction {
                    throw Exception()
                }
            }
        } catch (ignored: Exception) {
            /* empty */
        }

        verify(exactly = 1) { openConnection.rollback() }
    }

    @Test
    fun `transaction should re-throw exceptions`() {
        val context = CoroutineDataSource(dataSource)

        assertThrows<Exception> {
            runBlockingTest(context) {
                @Suppress("IMPLICIT_NOTHING_AS_TYPE_PARAMETER")
                transaction {
                    throw Exception()
                }
            }
        }
    }

    private suspend fun assertTransactionNotNull() {
        assertNotNull(coroutineContext.transaction)
    }

    private suspend fun assertTransactionEquals(expected: CoroutineTransaction) {
        assertEquals(expected, coroutineContext.transaction)
    }
}
