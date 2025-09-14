package com.github.michaelbull.jdbc.context

import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import javax.sql.DataSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CoroutineDataSourceTest {

    @Test
    fun `dataSource throws IllegalStateException if not in context`() = runTest {
        assertFailsWith<IllegalStateException> {
            coroutineContext.dataSource
        }
    }

    @Test
    fun `dataSource returns connection if in context`() = runTest {
        val expected = mockk<DataSource>()

        val actual = withContext(CoroutineDataSource(expected)) {
            coroutineContext.dataSource
        }

        assertEquals(expected, actual)
    }
}
