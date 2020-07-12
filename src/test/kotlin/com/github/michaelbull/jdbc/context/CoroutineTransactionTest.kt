package com.github.michaelbull.jdbc.context

import com.github.michaelbull.jdbc.TransactionState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CoroutineTransactionTest {

    @Test
    fun `CoroutineTransaction starts in idle state`() {
        val transaction = CoroutineTransaction()

        assertEquals(TransactionState.Idle, transaction.state)
    }

    @Test
    fun `CoroutineTransaction can initially be started`() {
        val transaction = CoroutineTransaction()
        transaction.start()

        assertEquals(TransactionState.Running, transaction.state)
    }

    @Test
    fun `CoroutineTransaction cannot be started more than once`() {
        val transaction = CoroutineTransaction()
        transaction.start()

        assertThrows<IllegalStateException> {
            transaction.start()
        }
    }

    @Test
    fun `CoroutineTransaction cannot be completed if not started`() {
        val transaction = CoroutineTransaction()

        assertThrows<IllegalStateException> {
            transaction.complete()
        }
    }

    @Test
    fun `CoroutineTransaction can be completed if running`() {
        val transaction = CoroutineTransaction()
        transaction.start()
        transaction.complete()

        assertEquals(TransactionState.Completed, transaction.state)
    }

    @Test
    fun `CoroutineTransaction cannot be completed more than once`() {
        val transaction = CoroutineTransaction()
        transaction.start()
        transaction.complete()

        assertThrows<IllegalStateException> {
            transaction.complete()
        }
    }

    @Test
    fun `CoroutineTransaction cannot be started if completed`() {
        val transaction = CoroutineTransaction()
        transaction.start()
        transaction.complete()

        assertThrows<IllegalStateException> {
            transaction.start()
        }
    }
}
