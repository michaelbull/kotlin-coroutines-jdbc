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
import kotlin.coroutines.coroutineContext

@ExperimentalCoroutinesApi
class ConnectionTest {

    private val openConnection = mockk<Connection>("OpenConnection", relaxed = true).apply {
        every { isClosed } returns false
    }

    private val closedConnection = mockk<Connection>("ClosedConnection", relaxed = true).apply {
        every { isClosed } returns true
    }

    private val dataSource = mockk<DataSource>(relaxed = true).apply {
        every { connection } returns openConnection
    }

    @Test
    fun `no longer fails to get connection inside withConnection`() {
        val context = CoroutineDataSource(dataSource)

        runBlockingTest(context) {
            withConnection {
                // the code inside the connection block would use the CoroutineContext
                // that was created before calling the withConnection function.

                // This happened because `this` referred to the parent CoroutineScope.

                // this will fail unless connection function returns its own scope
                // the scope of runBlockingTest was used before, which didn't have connection in context
                coroutineContext.connection
            }
        }
    }

    @Test
    fun `withConnection should add new connection to context if absent`() {
        val context = CoroutineDataSource(dataSource)

        runBlockingTest(context) {
            withConnection {
                assertConnectionNotNull()
            }
        }
    }

    @Test
    fun `withConnection should add new connection to context if existing is closed`() {
        val coroutineConnection = CoroutineConnection(closedConnection)
        val context = CoroutineDataSource(dataSource) + coroutineConnection

        runBlockingTest(context) {
            withConnection {
                assertConnectionNotNull()
                assertConnectionNotEquals(coroutineConnection)
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
                assertConnectionNotEquals(coroutineConnection)
            }
        }
    }

    @Test
    fun `withConnection should reuse existing connection if still open`() {
        val coroutineConnection = CoroutineConnection(openConnection)
        val context = CoroutineDataSource(dataSource) + coroutineConnection

        runBlockingTest(context) {
            withConnection {
                assertConnectionEquals(coroutineConnection)
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

    private suspend fun assertConnectionNotNull() {
        assertNotNull(coroutineContext.connection)
    }

    private suspend fun assertConnectionEquals(expected: Connection) {
        assertEquals(expected, coroutineContext.connection)
    }

    private suspend fun assertConnectionNotEquals(unexpected: Connection) {
        assertNotEquals(unexpected, coroutineContext.connection)
    }
}
