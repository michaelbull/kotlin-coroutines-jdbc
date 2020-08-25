package com.github.michaelbull.jdbc.context

import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.sql.Connection

@ExperimentalCoroutinesApi
class CoroutineConnectionTest {

    @Test
    fun `connection throws IllegalStateException if not in context`() {
        assertThrows<IllegalStateException> {
            runBlockingTest {
                coroutineContext.connection
            }
        }
    }

    @Test
    fun `connection returns connection if in context`() {
        val expected = mockk<Connection>()

        runBlockingTest(CoroutineConnection(expected)) {
            val actual = coroutineContext.connection
            assertEquals(expected, actual)
        }
    }
}
