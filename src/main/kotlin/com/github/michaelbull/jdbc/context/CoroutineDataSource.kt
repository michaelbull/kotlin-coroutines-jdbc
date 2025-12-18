package com.github.michaelbull.jdbc.context

import javax.sql.DataSource
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

public val CoroutineContext.dataSource: DataSource
    get() = get(CoroutineDataSource)?.dataSource ?: error("No data source in context")

public class CoroutineDataSource(
    public val dataSource: DataSource,
) : AbstractCoroutineContextElement(CoroutineDataSource) {

    public companion object Key : CoroutineContext.Key<CoroutineDataSource>

    override fun toString(): String {
        return "CoroutineDataSource($dataSource)"
    }
}
