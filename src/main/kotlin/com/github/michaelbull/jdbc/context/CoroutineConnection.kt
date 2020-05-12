package com.github.michaelbull.jdbc.context

import java.sql.Connection
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

val CoroutineContext.connection: Connection
    get() = get(CoroutineConnection) ?: error("No connection in context")

@PublishedApi
internal class CoroutineConnection(
    private val connection: Connection
) : AbstractCoroutineContextElement(CoroutineConnection), Connection by connection {

    companion object Key : CoroutineContext.Key<CoroutineConnection>

    override fun toString() = "CoroutineConnection($connection)"
}
