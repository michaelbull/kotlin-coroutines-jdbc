package com.github.michaelbull.jdbc

import com.github.michaelbull.jdbc.context.CoroutineConnection
import com.github.michaelbull.jdbc.context.CoroutineDataSource
import com.github.michaelbull.jdbc.context.connection
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.sql.Connection
import java.sql.SQLException
import javax.sql.DataSource

@ExperimentalCoroutinesApi
class ConnectionTest {

    @Test
    fun `withConnection throws IllegalStateException if no connection & dataSource in context`() {
        assertThrows<IllegalStateException> {
            runBlockingTest {
                withConnection {

                }
            }
        }
    }

    @Test
    fun `withConnection adds new connection to context if no connection in context`() {
        val newConnection = mockk<Connection> {
            every { close() } just runs
        }

        val dataSource = mockk<DataSource> {
            every { connection } returns newConnection
        }

        val context = CoroutineDataSource(dataSource)

        runBlockingTest(context) {
            val actual = withConnection {
                coroutineContext.connection
            }

            assertEquals(newConnection, actual)
        }
    }

    @Test
    fun `withConnection adds new connection to context if existing connection isClosed returns true`() {
        val existingConnection = mockk<Connection> {
            every { isClosed } returns true
        }

        val newConnection = mockk<Connection> {
            every { close() } just runs
        }

        val dataSource = mockk<DataSource> {
            every { connection } returns newConnection
        }

        val context = CoroutineDataSource(dataSource) + CoroutineConnection(existingConnection)

        runBlockingTest(context) {
            val actual = withConnection {
                coroutineContext.connection
            }

            assertEquals(newConnection, actual)
        }
    }

    @Test
    fun `withConnection adds new connection to context if existing connection isClosed throws exception`() {
        val existingConnection = mockk<Connection> {
            every { isClosed } throws SQLException()
        }

        val newConnection = mockk<Connection> {
            every { close() } just runs
        }

        val dataSource = mockk<DataSource> {
            every { connection } returns newConnection
        }

        val context = CoroutineDataSource(dataSource) + CoroutineConnection(existingConnection)

        runBlockingTest(context) {
            val actual = withConnection {
                coroutineContext.connection
            }

            assertEquals(newConnection, actual)
        }
    }

    @Test
    fun `withConnection reuses existing connection in context if not closed`() {
        val existing = mockk<Connection> {
            every { isClosed } returns false
        }

        val dataSource = mockk<DataSource>()
        val context = CoroutineDataSource(dataSource) + CoroutineConnection(existing)

        runBlockingTest(context) {
            val actual = withConnection {
                coroutineContext.connection
            }

            assertEquals(existing, actual)
        }
    }

    @Test
    fun `withConnection closes connection if added to context`() {
        val newConnection = mockk<Connection> {
            every { close() } just runs
        }

        val dataSource = mockk<DataSource> {
            every { connection } returns newConnection
        }

        val context = CoroutineDataSource(dataSource)

        runBlockingTest(context) {
            withConnection { }
        }

        verify(exactly = 1) { newConnection.close() }
    }

    @Test
    fun `withConnection ignores SQLExceptions when closing connection added to context`() {
        val newConnection = mockk<Connection> {
            every { close() } throws SQLException()
        }

        val dataSource = mockk<DataSource> {
            every { connection } returns newConnection
        }

        val context = CoroutineDataSource(dataSource)

        runBlockingTest(context) {
            withConnection { }
        }

        verify(exactly = 1) { newConnection.close() }
    }

    @Test
    fun `withConnection does not close connection if connection was not added to context`() {
        val existing = mockk<Connection> {
            every { isClosed } returns false
        }

        val dataSource = mockk<DataSource> {
            every { connection } returns existing
        }

        val context = CoroutineDataSource(dataSource) + CoroutineConnection(existing)

        runBlockingTest(context) {
            withConnection { }
        }

        verify(exactly = 0) { existing.close() }
    }
}
