package com.github.michaelbull.jdbc

import com.github.michaelbull.jdbc.context.CoroutineConnection
import com.github.michaelbull.jdbc.context.CoroutineDataSource
import com.github.michaelbull.jdbc.context.connection
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.SQLException
import javax.sql.DataSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ConnectionTest {

    @Test
    fun `withConnection throws IllegalStateException if no connection & dataSource in context`() = runTest {
        assertFailsWith<IllegalStateException> {
            withConnection {
                /* empty */
            }
        }
    }


    @Test
    fun `withConnection adds new connection to context if no connection in context`() = runTest {
        val newConnection = mockk<Connection> {
            every { close() } just runs
        }

        val dataSource = mockk<DataSource> {
            every { connection } returns newConnection
        }

        withContext(CoroutineDataSource(dataSource)) {
            val actual = withConnection {
                coroutineContext.connection
            }

            assertEquals(newConnection, actual)
        }
    }

    @Test
    fun `withConnection adds new connection to context if existing connection isClosed returns true`() = runTest {
        val existingConnection = mockk<Connection> {
            every { isClosed } returns true
        }

        val newConnection = mockk<Connection> {
            every { close() } just runs
        }

        val dataSource = mockk<DataSource> {
            every { connection } returns newConnection
        }

        withContext(CoroutineDataSource(dataSource) + CoroutineConnection(existingConnection)) {
            val actual = withConnection {
                coroutineContext.connection
            }

            assertEquals(newConnection, actual)
        }
    }

    @Test
    fun `withConnection adds new connection to context if existing connection isClosed throws exception`() = runTest {
        val existingConnection = mockk<Connection> {
            every { isClosed } throws SQLException()
        }

        val newConnection = mockk<Connection> {
            every { close() } just runs
        }

        val dataSource = mockk<DataSource> {
            every { connection } returns newConnection
        }

        withContext(CoroutineDataSource(dataSource) + CoroutineConnection(existingConnection)) {
            val actual = withConnection {
                coroutineContext.connection
            }

            assertEquals(newConnection, actual)
        }
    }

    @Test
    fun `withConnection reuses existing connection in context if not closed`() = runTest {
        val existing = mockk<Connection> {
            every { isClosed } returns false
        }

        val dataSource = mockk<DataSource>()

        withContext(CoroutineDataSource(dataSource) + CoroutineConnection(existing)) {
            val actual = withConnection {
                coroutineContext.connection
            }

            assertEquals(existing, actual)
        }
    }

    @Test
    fun `withConnection closes connection if added to context`() = runTest {
        val newConnection = mockk<Connection> {
            every { close() } just runs
        }

        val dataSource = mockk<DataSource> {
            every { connection } returns newConnection
        }

        val context = CoroutineDataSource(dataSource)

        withContext(context) {
            withConnection {
                /* empty */
            }
        }

        verify(exactly = 1) { newConnection.close() }
    }

    @Test
    fun `withConnection ignores SQLExceptions when closing connection added to context`() = runTest {
        val newConnection = mockk<Connection> {
            every { close() } throws SQLException()
        }

        val dataSource = mockk<DataSource> {
            every { connection } returns newConnection
        }

        withContext(CoroutineDataSource(dataSource)) {
            withConnection {
                /* empty */
            }
        }

        verify(exactly = 1) { newConnection.close() }
    }

    @Test
    fun `withConnection does not close connection if connection was not added to context`() = runTest {
        val existing = mockk<Connection> {
            every { isClosed } returns false
        }

        val dataSource = mockk<DataSource> {
            every { connection } returns existing
        }

        withContext(CoroutineDataSource(dataSource) + CoroutineConnection(existing)) {
            withConnection {
                /* empty */
            }
        }

        verify(exactly = 0) { existing.close() }
    }
}
