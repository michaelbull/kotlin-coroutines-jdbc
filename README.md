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

Calls to `transaction` can be nested inside another, with each child re-use the
first `CoroutineTransaction`. Only the outermost call will either
[`commit`][Connection.commit] or [`rollback`][Connection.rollback] the
transaction.

Starting a fresh transaction will add a
[`CoroutineTransaction`][CoroutineTransaction] to the current
[`CoroutineContext`][CoroutineContext]. Transactions cannot be re-used after
completion and attempting to do so will result in a runtime failure.

A transaction will establish a new [`Connection`][Connection] if an open one
does not already exist in the active [`CoroutineContext`][CoroutineContext].
If the transaction does establish a new [`Connection`][Connection], it will
attempt to [close][Connection.close] it upon completion.

An active [`CoroutineConnection`][CoroutineConnection] is accessible from the
current [`CoroutineContext`][CoroutineContext]. The connection from the context
can be used to [prepare statements][Connection.prepareStatement].

## Example

```kotlin
import com.github.michaelbull.jdbc.context.CoroutineDataSource
import com.github.michaelbull.jdbc.context.connection
import com.github.michaelbull.jdbc.transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.sql.DataSource
import kotlin.coroutines.coroutineContext

class Example(dataSource: DataSource) {

    private val scope = CoroutineScope(Dispatchers.IO + CoroutineDataSource(dataSource))
    private val repository = Repository()

    fun query() {
        scope.launchTransaction()
    }

    private fun CoroutineScope.launchTransaction() = launch {
        val customers = addThenFindAllCustomers()
        customers.forEach(::println)
    }

    private suspend fun addThenFindAllCustomers(): List<String> {
        return transaction {
            repository.addCustomer("example name")
            repository.findAllCustomers()
        }
    }
}

class Repository {

    suspend fun addCustomer(name: String) {
        coroutineContext.connection.prepareStatement("INSERT INTO customers VALUES (?)").use { stmt ->
            stmt.setString(1, name)
            stmt.executeUpdate()
        }
    }

    suspend fun findAllCustomers(): List<String> {
        val customers = mutableListOf<String>()

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
[Connection.close]: https://docs.oracle.com/javase/8/docs/api/java/sql/Connection.html#close--
[Connection.isClosed]: https://docs.oracle.com/javase/8/docs/api/java/sql/Connection.html#isClosed--
[Connection.commit]: https://docs.oracle.com/javase/8/docs/api/java/sql/Connection.html#commit--
[Connection.rollback]: https://docs.oracle.com/javase/8/docs/api/java/sql/Connection.html#rollback--
[Connection.prepareStatement]: https://docs.oracle.com/javase/8/docs/api/java/sql/Connection.html#prepareStatement-java.lang.String-
[CoroutineConnection]: ./src/main/kotlin/com/github/michaelbull/jdbc/context/CoroutineConnection.kt
[CoroutineContext]: https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.coroutines/-coroutine-context/
