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

        val actual = withContext(CoroutineDataSource(dataSource)) {
            withConnection {
                coroutineContext.connection
            }
        }

        assertEquals(newConnection, actual)
    }

    @Test
    fun `withConnection adds new connection to context if existing connection isClosed returns true`() = runTest {
        val closedConnection = mockk<Connection> {
            every { isClosed } returns true
        }

        val newConnection = mockk<Connection> {
            every { close() } just runs
        }

        val dataSource = mockk<DataSource> {
            every { connection } returns newConnection
        }

        val actual = withContext(CoroutineDataSource(dataSource) + CoroutineConnection(closedConnection)) {
            withConnection {
                coroutineContext.connection
            }
        }

        assertEquals(newConnection, actual)
    }

    @Test
    fun `withConnection adds new connection to context if existing connection isClosed throws exception`() = runTest {
        val brokenConnection = mockk<Connection> {
            every { isClosed } throws SQLException()
        }

        val newConnection = mockk<Connection> {
            every { close() } just runs
        }

        val dataSource = mockk<DataSource> {
            every { connection } returns newConnection
        }

        val actual = withContext(CoroutineDataSource(dataSource) + CoroutineConnection(brokenConnection)) {
            withConnection {
                coroutineContext.connection
            }
        }

        assertEquals(newConnection, actual)
    }

    @Test
    fun `withConnection reuses open connection`() = runTest {
        val openConnection = mockk<Connection> {
            every { isClosed } returns false
        }

        val dataSource = mockk<DataSource>()

        val actual = withContext(CoroutineDataSource(dataSource) + CoroutineConnection(openConnection)) {
            withConnection {
                coroutineContext.connection
            }
        }

        assertEquals(openConnection, actual)
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
        val openConnection = mockk<Connection> {
            every { isClosed } returns false
        }

        val dataSource = mockk<DataSource> {
            every { connection } returns openConnection
        }

        withContext(CoroutineDataSource(dataSource) + CoroutineConnection(openConnection)) {
            withConnection {
                /* empty */
            }
        }

        verify(exactly = 0) { openConnection.close() }
    }
}
