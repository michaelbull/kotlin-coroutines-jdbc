# kotlin-coroutines-jdbc

[![Maven Central](https://img.shields.io/maven-central/v/com.michael-bull.kotlin-coroutines-jdbc/kotlin-coroutines-jdbc.svg)](https://search.maven.org/search?q=g:com.michael-bull.kotlin-coroutines-jdbc)
[![CI](https://github.com/michaelbull/kotlin-coroutines-jdbc/actions/workflows/ci.yaml/badge.svg)](https://github.com/michaelbull/kotlin-coroutines-jdbc/actions/workflows/ci.yaml)
[![License](https://img.shields.io/github/license/michaelbull/kotlin-coroutines-jdbc.svg)](https://github.com/michaelbull/kotlin-coroutines-jdbc/blob/master/LICENSE)

A library for interacting with blocking JDBC drivers using [Kotlin Coroutines][coroutines].

Use of this library allows you to offload blocking JDBC calls to a dedicated
[`CoroutineDispatcher`][CoroutineDispatcher] (e.g. [`Dispatchers.IO`][Dispatchers.IO]), thus suspending your coroutine
and freeing your thread for other work while waiting.

## Installation

```groovy
repositories {
    mavenCentral()
}

dependencies {
    implementation("com.michael-bull.kotlin-coroutines-jdbc:kotlin-coroutines-jdbc:1.0.3")
}
```

## Introduction

The primary higher-order function exposed by the library is the [`transaction`][transaction] function.

```kotlin
suspend inline fun <T> transaction(crossinline block: suspend () -> T): T
```

Calling this function with a specific suspending block will run the block in the context of a
[`CoroutineTransaction`][CoroutineTransaction].

Transactions **cannot be nested**. Calling `transaction` (or any of its variants) within an existing transaction will
throw an [`IllegalStateException`][IllegalStateException].

Starting a transaction will add a [`CoroutineTransaction`][CoroutineTransaction] to the current
[`CoroutineContext`][CoroutineContext]. The transaction will either [`commit`][Connection.commit] on success or
[`rollback`][Connection.rollback] if an exception is thrown.

A transaction will establish a new [`Connection`][Connection] if an open one does not already exist in the active
[`CoroutineContext`][CoroutineContext]. If the transaction does establish a new [`Connection`][Connection], it will
attempt to [`close`][Connection.close] it upon completion.

An active [`Connection`][Connection] is accessible via the [`currentConnection`][currentConnection] function, which can
be used to [prepare statements][Connection.prepareStatement].

### Transaction Variants

The library provides several transaction variants for different use cases:

- **`transaction`** - Standard read-write transaction
- **`readOnlyTransaction`** - Sets the connection to [read-only mode][Connection.setReadOnly] before starting the
  transaction
- **`isolatedTransaction`** - Sets a specific [transaction isolation level][Connection.setTransactionIsolation] before
  starting the transaction
- **`isolatedReadOnlyTransaction`** - Combines read-only mode with a specific isolation level

```kotlin
// Standard transaction
transaction {
    // read-write operations
}

// Read-only transaction (useful for queries that don't modify data)
readOnlyTransaction {
    // read-only operations
}

// Transaction with specific isolation level
isolatedTransaction(Connection.TRANSACTION_SERIALIZABLE) {
    // operations with serializable isolation
}

// Read-only transaction with specific isolation level
isolatedReadOnlyTransaction(Connection.TRANSACTION_READ_COMMITTED) {
    // read-only operations with read committed isolation
}
```

## Example

```kotlin
import com.github.michaelbull.jdbc.context.CoroutineDataSource
import com.github.michaelbull.jdbc.currentConnection
import com.github.michaelbull.jdbc.transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.sql.DataSource

class Example(dataSource: DataSource) {

    private val scope = CoroutineScope(Dispatchers.IO + CoroutineDataSource(dataSource))
    private val customers = CustomerRepository()

    fun query() {
        scope.launchTransaction()
    }

    private fun CoroutineScope.launchTransaction() = launch {
        val customers = addThenFindAllCustomers()
        customers.forEach(::println)
    }

    private suspend fun addThenFindAllCustomers(): List<String> = transaction {
        customers.add("John Doe")
        customers.findAll()
    }
}

class CustomerRepository {

    suspend fun add(name: String) {
        currentConnection().prepareStatement("INSERT INTO customers VALUES (?)").use { stmt ->
            stmt.setString(1, name)
            stmt.executeUpdate()
        }
    }

    suspend fun findAll() = buildList<String> {
        urrentConnection().prepareStatement("SELECT name FROM customers").use { stmt ->
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    add(rs.getString("name"))
                }
            }
        }
    }
}
```

## Further Reading

- [andrewoma/coroutine-jdbc](https://github.com/andrewoma/coroutine-jdbc)
- [Coroutine Context and Scope - Roman Elizarov](https://medium.com/@elizarov/coroutine-context-and-scope-c8b255d59055)
- [Blocking threads, suspending coroutines - Roman Elizarov](https://medium.com/@elizarov/blocking-threads-suspending-coroutines-d33e11bf4761)

## Contributing

Bug reports and pull requests are welcome on [GitHub][github].

## License

This project is available under the terms of the ISC license. See the [`LICENSE`](LICENSE) file for the copyright
information and licensing terms.

[//]: # (@formatter:off)
[coroutines]: https://kotlinlang.org/docs/reference/coroutines-overview.html
[CoroutineDispatcher]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-coroutine-dispatcher/index.html
[Dispatchers.IO]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-dispatchers/-i-o.html
[transaction]: https://github.com/michaelbull/kotlin-coroutines-jdbc/blob/master/src/main/kotlin/com/github/michaelbull/jdbc/Transaction.kt
[CoroutineTransaction]: https://github.com/michaelbull/kotlin-coroutines-jdbc/blob/master/src/main/kotlin/com/github/michaelbull/jdbc/context/CoroutineTransaction.kt
[Connection.commit]: https://docs.oracle.com/javase/8/docs/api/java/sql/Connection.html#commit--
[Connection.rollback]: https://docs.oracle.com/javase/8/docs/api/java/sql/Connection.html#rollback--
[Connection.setReadOnly]: https://docs.oracle.com/javase/8/docs/api/java/sql/Connection.html#setReadOnly-boolean-
[Connection.setTransactionIsolation]: https://docs.oracle.com/javase/8/docs/api/java/sql/Connection.html#setTransactionIsolation-int-
[CoroutineContext]: https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.coroutines/-coroutine-context/
[Connection]: https://docs.oracle.com/javase/8/docs/api/java/sql/Connection.html
[Connection.close]: https://docs.oracle.com/javase/8/docs/api/java/sql/Connection.html#close--
[currentConnection]: https://github.com/michaelbull/kotlin-coroutines-jdbc/blob/master/src/main/kotlin/com/github/michaelbull/jdbc/Connection.kt
[Connection.prepareStatement]: https://docs.oracle.com/javase/8/docs/api/java/sql/Connection.html#prepareStatement-java.lang.String-
[IllegalStateException]: https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-illegal-state-exception/
[github]: https://github.com/michaelbull/kotlin-coroutines-jdbc
[//]: # (@formatter:on)
