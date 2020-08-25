package com.github.michaelbull.jdbc.context

import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import javax.sql.DataSource

@ExperimentalCoroutinesApi
class CoroutineDataSourceTest {

    @Test
    fun `dataSource throws IllegalStateException if not in context`() {
        assertThrows<IllegalStateException> {
            runBlockingTest {
                coroutineContext.dataSource
            }
        }
    }

    @Test
    fun `dataSource returns connection if in context`() {
        val expected = mockk<DataSource>()

        runBlockingTest(CoroutineDataSource(expected)) {
            val actual = coroutineContext.dataSource
            Assertions.assertEquals(expected, actual)
        }
    }
}
