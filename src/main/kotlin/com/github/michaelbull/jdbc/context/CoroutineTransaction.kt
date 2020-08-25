package com.github.michaelbull.jdbc.context

import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

@PublishedApi
internal class CoroutineTransaction(
    private var completed: Boolean = false
) : AbstractCoroutineContextElement(CoroutineTransaction) {

    companion object Key : CoroutineContext.Key<CoroutineTransaction>

    val incomplete: Boolean
        get() = !completed

    fun complete() {
        completed = true
    }

    override fun toString(): String = "CoroutineTransaction(completed=$completed)"
}
