package com.github.michaelbull.jdbc.context

import java.sql.Connection
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

public val CoroutineContext.connection: Connection
    get() = get(CoroutineConnection)?.connection ?: error("No connection in context")

public class CoroutineConnection(
    public val connection: Connection,
) : AbstractCoroutineContextElement(CoroutineConnection) {

    public companion object Key : CoroutineContext.Key<CoroutineConnection>

    override fun toString(): String {
        return "CoroutineConnection($connection)"
    }
}
