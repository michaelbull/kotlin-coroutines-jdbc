package com.github.michaelbull.jdbc

import com.github.michaelbull.jdbc.context.CoroutineConnection
import com.github.michaelbull.jdbc.context.CoroutineDataSource
import com.github.michaelbull.jdbc.context.connection
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.sql.SQLException
import javax.sql.DataSource

@ExperimentalCoroutinesApi
class ConnectionTest {

    private val openConnection = mockk<Connection>("OpenConnection", relaxed = true) {
        every { isClosed } returns false
    }

    private val closedConnection = mockk<Connection>("ClosedConnection", relaxed = true) {
        every { isClosed } returns true
    }

    private val dataSource = mockk<DataSource>(relaxed = true).apply {
        every { connection } returns openConnection
    }

    @Test
    fun `withConnection should add new connection to context if absent`() {
        val context = CoroutineDataSource(dataSource)

        runBlockingTest(context) {
            withConnection {
                assertNotNull(coroutineContext.connection)
            }
        }
    }

    @Test
    fun `withConnection should add new connection to context if existing is closed`() {
        val coroutineConnection = CoroutineConnection(closedConnection)
        val context = CoroutineDataSource(dataSource) + coroutineConnection

        runBlockingTest(context) {
            withConnection {
                assertNotNull(coroutineContext.connection)
                assertNotEquals(coroutineConnection, coroutineContext.connection)
            }
        }
    }

    @Test
    fun `withConnection should add new connection to context if isClosed check throws exception`() {
        val coroutineConnection = CoroutineConnection(openConnection)
        val context = CoroutineDataSource(dataSource) + coroutineConnection

        every { openConnection.isClosed } throws SQLException()

        runBlockingTest(context) {
            withConnection {
                assertNotEquals(coroutineConnection, coroutineContext.connection)
            }
        }
    }

    @Test
    fun `withConnection should reuse existing connection if still open`() {
        val coroutineConnection = CoroutineConnection(openConnection)
        val context = CoroutineDataSource(dataSource) + coroutineConnection

        runBlockingTest(context) {
            withConnection {
                assertEquals(coroutineConnection, coroutineContext.connection)
            }
        }
    }

    @Test
    fun `withConnection should close connection if new one created`() {
        val context = CoroutineDataSource(dataSource)

        runBlockingTest(context) {
            withConnection { }
        }

        verify(exactly = 1) { openConnection.close() }
    }

    @Test
    fun `withConnection should ignore failure when closing connection`() {
        val context = CoroutineDataSource(dataSource)

        every { openConnection.close() } throws SQLException()

        runBlockingTest(context) {
            withConnection { }
        }

        verify(exactly = 1) { openConnection.close() }
    }

    @Test
    fun `withConnection should not close connection if reusing existing connection`() {
        val context = CoroutineDataSource(dataSource) + CoroutineConnection(openConnection)

        runBlockingTest(context) {
            withConnection { }
        }

        verify(exactly = 0) { openConnection.close() }
    }
}
