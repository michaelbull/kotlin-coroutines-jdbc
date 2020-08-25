package com.github.michaelbull.jdbc.context

import java.sql.Connection
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

val CoroutineContext.connection: Connection
    get() = get(CoroutineConnection)?.connection ?: error("No connection in context")

class CoroutineConnection(
    val connection: Connection
) : AbstractCoroutineContextElement(CoroutineConnection) {

    companion object Key : CoroutineContext.Key<CoroutineConnection>

    override fun toString() = "CoroutineConnection($connection)"
}
