package com.github.michaelbull.jdbc.context

import kotlin.coroutines.CoroutineContext

@PublishedApi
internal object CoroutineTransaction : CoroutineContext.Element, CoroutineContext.Key<CoroutineTransaction> {
    override val key: CoroutineContext.Key<CoroutineTransaction>
        get() = this
}
