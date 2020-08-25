package com.github.michaelbull.jdbc.context

import javax.sql.DataSource
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

val CoroutineContext.dataSource: DataSource
    get() = get(CoroutineDataSource)?.dataSource ?: error("No data source in context")

class CoroutineDataSource(
    val dataSource: DataSource
) : AbstractCoroutineContextElement(CoroutineDataSource) {

    companion object Key : CoroutineContext.Key<CoroutineDataSource>

    override fun toString() = "CoroutineDataSource($dataSource)"
}
