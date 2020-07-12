# kotlin-coroutines-jdbc

[![Maven Central](https://img.shields.io/maven-central/v/com.michael-bull.kotlin-coroutines-jdbc/kotlin-coroutines-jdbc.svg)](https://search.maven.org/search?q=g:com.michael-bull.kotlin-coroutines-jdbc) [![CI Status](https://github.com/michaelbull/kotlin-coroutines-jdbc/workflows/ci/badge.svg)](https://github.com/michaelbull/kotlin-coroutines-jdbc/actions?query=workflow%3Aci) [![License](https://img.shields.io/github/license/michaelbull/kotlin-coroutines-jdbc.svg)](https://github.com/michaelbull/kotlin-coroutines-jdbc/blob/master/LICENSE)

A library for interacting with blocking JDBC drivers using [Kotlin Coroutines][coroutines].

## Installation

```groovy
repositories {
    mavenCentral()
}

dependencies {
    implementation("com.michael-bull.kotlin-coroutines-jdbc:kotlin-coroutines-jdbc:1.0.0")
}
```

## Introduction

The primary higher-order function exposed by the library is the
[`transaction`][transaction] function.

```kotlin
suspend inline fun <T> transaction(crossinline block: suspend () -> T): T
```

Calling this function with a specific suspending block will run the block in
the context of a [`CoroutineTransaction`][CoroutineTransaction].

Calls to `transaction` can be composed, and will re-use the existing
`CoroutineTransaction`. Only the outermost call will either
[`commit`][Connection.commit] or [`rollback`][Connection.rollback] the result.

It is therefore safe to nest calls to `transaction`, or have functions compose
of multiple calls to `transaction` without having to manage the boundaries of
the transaction yourself.

Starting a fresh transaction will add a
[`CoroutineTransaction`][CoroutineTransaction] to the current
[`CoroutineContext`][CoroutineContext]. A finite state machine backs each
transaction to prevent it from being erroneously re-used after its completion.

A transaction will operate in the context of a [`Connection`][Connection].
The `transaction` function adds a [`CoroutineConnection`][CoroutineConnection]
to the active [`CoroutineContext`][CoroutineContext] if it is not already
present or if the existing one [is closed][Connection.isClosed]. If the
transaction establishes a new connection and is not re-using an existing one,
it will attempt to cleanly close the connection within its completion state.

An active [`CoroutineConnection`][CoroutineConnection] is accessible from the
current [`CoroutineContext`][CoroutineContext]. The connection from the context
can be used to [prepare statements][Connection.prepareStatement].

## Example

```kotlin
import com.github.michaelbull.jdbc.context.connection
import com.github.michaelbull.jdbc.context.CoroutineDataSource
import com.github.michaelbull.jdbc.transaction
import java.util.concurrent.Executors
import javax.sql.DataSource
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope

class Example(dataSource: DataSource) {

    private val dispatcher = Executors.newFixedThreadPool(8).asCoroutineDispatcher()
    private val scope = CoroutineScope(dispatcher + CoroutineDataSource(dataSource))

    fun query() {
        scope.launchTransaction()
    }

    private fun CoroutineScope.launchTransaction() = launch {
        val customers = addThenFindAllCustomers()
        customers.forEach(::println)
    }

    private suspend fun addThenFindAllCustomers(): List<String>  {
        return transaction {
            addCustomer("example name")
            findAllCustomers()
        }
    }

    private suspend fun addCustomer(name: String) {
        coroutineContext.connection.prepareStatement("INSERT INTO customers VALUES (?)").use { stmt ->
            stmt.setString(1, name)
            stmt.executeUpdate()
        }
    }

    private suspend fun findAllCustomers(): List<String> {
        val customers = mutableListOf<String>

        coroutineContext.connection.prepareStatement("SELECT name FROM customers").use { stmt ->
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    customers += rs.getString("name")
                }
            }
        }

        return customers
    }
}
```

## Contributing

Bug reports and pull requests are welcome on [GitHub][github].

## License

This project is available under the terms of the ISC license. See the
[`LICENSE`](LICENSE) file for the copyright information and licensing terms.

[github]: https://github.com/michaelbull/kotlin-coroutines-jdbc
[coroutines]: https://kotlinlang.org/docs/reference/coroutines-overview.html
[transaction]: ./src/main/kotlin/com/github/michaelbull/jdbc/Transaction.kt
[CoroutineTransaction]: ./src/main/kotlin/com/github/michaelbull/jdbc/context/CoroutineTransaction.kt
[Connection]: https://docs.oracle.com/javase/8/docs/api/java/sql/Connection.html
[Connection.isClosed]: https://docs.oracle.com/javase/8/docs/api/java/sql/Connection.html#isClosed--
[Connection.commit]: https://docs.oracle.com/javase/8/docs/api/java/sql/Connection.html#commit--
[Connection.rollback]: https://docs.oracle.com/javase/8/docs/api/java/sql/Connection.html#rollback--
[Connection.prepareStatement]: https://docs.oracle.com/javase/8/docs/api/java/sql/Connection.html#prepareStatement-java.lang.String-
[CoroutineConnection]: ./src/main/kotlin/com/github/michaelbull/jdbc/context/CoroutineConnection.kt
[CoroutineContext]: https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.coroutines/-coroutine-context/
