package com.github.michaelbull.jdbc

import com.github.michaelbull.jdbc.context.CoroutineConnection
import com.github.michaelbull.jdbc.context.CoroutineDataSource
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import io.mockk.verifySequence
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.Connection.TRANSACTION_READ_COMMITTED
import java.sql.Connection.TRANSACTION_SERIALIZABLE
import java.sql.SQLException
import javax.sql.DataSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ConnectionTest {

    @Test
    fun `withConnection throws IllegalStateException if no connection or dataSource in context`() = runTest {
        assertFailsWith<IllegalStateException> {
            withConnection {
                /* empty */
            }
        }
    }

    @Test
    fun `withConnection creates new connection from dataSource if no connection in context`() = runTest {
        val newConnection = mockk<Connection> {
            every { close() } just runs
        }

        val dataSource = mockk<DataSource> {
            every { connection } returns newConnection
        }

        val actual = withContext(CoroutineDataSource(dataSource)) {
            withConnection {
                this
            }
        }

        assertEquals(newConnection, actual)
    }

    @Test
    fun `withConnection creates new connection if existing connection is closed`() = runTest {
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
                this
            }
        }

        assertEquals(newConnection, actual)
    }

    @Test
    fun `withConnection creates new connection if existing connection isClosed throws exception`() = runTest {
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
                this
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
                this
            }
        }

        assertEquals(openConnection, actual)
    }

    @Test
    fun `withConnection closes new connection after block completes`() = runTest {
        val newConnection = mockk<Connection> {
            every { close() } just runs
        }

        val dataSource = mockk<DataSource> {
            every { connection } returns newConnection
        }

        withContext(CoroutineDataSource(dataSource)) {
            withConnection {
                /* empty */
            }
        }

        verify(exactly = 1) {
            newConnection.close()
        }
    }

    @Test
    fun `withConnection catches SQLException when closing connection`() = runTest {
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

        verify(exactly = 1) {
            newConnection.close()
        }
    }

    @Test
    fun `withConnection does not close reused connection`() = runTest {
        val openConnection = mockk<Connection> {
            every { isClosed } returns false
        }

        val dataSource = mockk<DataSource>()

        withContext(CoroutineDataSource(dataSource) + CoroutineConnection(openConnection)) {
            withConnection {
                /* empty */
            }
        }

        verify(exactly = 0) {
            openConnection.close()
        }
    }

    @Test
    fun `withManualCommit sets autoCommit to false`() {
        val connection = mockk<Connection> {
            every { autoCommit } returns true
            every { autoCommit = any() } just runs
        }

        connection.withManualCommit {
            verify(exactly = 1) {
                connection.autoCommit = false
            }
        }
    }

    @Test
    fun `withManualCommit restores autoCommit after block completes`() {
        val connection = mockk<Connection> {
            every { autoCommit } returns true
            every { autoCommit = any() } just runs
        }

        connection.withManualCommit {
            /* empty */
        }

        verifySequence {
            connection.autoCommit
            connection.autoCommit = false
            connection.autoCommit = true
        }
    }

    @Test
    fun `withReadOnly sets readOnly to true`() {
        val connection = mockk<Connection> {
            every { isReadOnly } returns false
            every { isReadOnly = any() } just runs
        }

        connection.withReadOnly {
            verify(exactly = 1) {
                connection.isReadOnly = true
            }
        }
    }

    @Test
    fun `withReadOnly restores readOnly after block completes`() {
        val connection = mockk<Connection> {
            every { isReadOnly } returns false
            every { isReadOnly = any() } just runs
        }

        connection.withReadOnly {
            /* empty */
        }

        verifySequence {
            connection.isReadOnly
            connection.isReadOnly = true
            connection.isReadOnly = false
        }
    }

    @Test
    fun `withIsolation sets transactionIsolation`() {
        val connection = mockk<Connection> {
            every { transactionIsolation } returns TRANSACTION_READ_COMMITTED
            every { transactionIsolation = any() } just runs
        }

        connection.withIsolation(TRANSACTION_SERIALIZABLE) {
            verify(exactly = 1) {
                connection.transactionIsolation = TRANSACTION_SERIALIZABLE
            }
        }
    }

    @Test
    fun `withIsolation restores transactionIsolation after block completes`() {
        val connection = mockk<Connection> {
            every { transactionIsolation } returns TRANSACTION_READ_COMMITTED
            every { transactionIsolation = any() } just runs
        }

        connection.withIsolation(TRANSACTION_SERIALIZABLE) {
            /* empty */
        }

        verifySequence {
            connection.transactionIsolation
            connection.transactionIsolation = TRANSACTION_SERIALIZABLE
            connection.transactionIsolation = TRANSACTION_READ_COMMITTED
        }
    }

    @Test
    fun `currentConnection returns connection from context`() = runTest {
        val connection = mockk<Connection>()

        val actual = withContext(CoroutineConnection(connection)) {
            currentConnection()
        }

        assertEquals(connection, actual)
    }

    @Test
    fun `currentConnection throws IllegalStateException if no connection in context`() = runTest {
        assertFailsWith<IllegalStateException> {
            currentConnection()
        }
    }
}
