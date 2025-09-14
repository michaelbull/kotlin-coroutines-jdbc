package com.github.michaelbull.jdbc.context

import kotlin.test.Test
import kotlin.test.assertFalse

class CoroutineTransactionTest {

    @Test
    fun `CoroutineTransaction can be completed`() {
        val transaction = CoroutineTransaction()
        transaction.complete()
        assertFalse(transaction.incomplete)
    }
}
