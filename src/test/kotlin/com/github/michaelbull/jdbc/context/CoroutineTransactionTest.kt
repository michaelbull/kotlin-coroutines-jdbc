package com.github.michaelbull.jdbc.context

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class CoroutineTransactionTest {

    @Test
    fun `CoroutineTransaction can be completed`() {
        val transaction = CoroutineTransaction()
        transaction.complete()
        assertFalse(transaction.incomplete)
    }
}
