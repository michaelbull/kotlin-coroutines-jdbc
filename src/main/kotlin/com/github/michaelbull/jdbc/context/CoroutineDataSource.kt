package com.github.michaelbull.jdbc.context

import javax.sql.DataSource
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

val CoroutineContext.dataSource: DataSource?
    get() = get(CoroutineDataSource)

internal class CoroutineDataSource(
    private val dataSource: DataSource
) : AbstractCoroutineContextElement(CoroutineDataSource), DataSource by dataSource {

    companion object Key : CoroutineContext.Key<CoroutineDataSource>

    override fun toString() = "CoroutineDataSource($dataSource)"
}
