package com.github.michaelbull.jdbc.context

import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import java.sql.Connection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CoroutineConnectionTest {

    @Test
    fun `connection throws IllegalStateException if not in context`() = runTest {
        assertFailsWith<IllegalStateException> {
            coroutineContext.connection
        }
    }

    @Test
    fun `connection returns connection if in context`() = runTest {
        val expected = mockk<Connection>()

        withContext(CoroutineConnection(expected)) {
            val actual = coroutineContext.connection
            assertEquals(expected, actual)
        }
    }
}
