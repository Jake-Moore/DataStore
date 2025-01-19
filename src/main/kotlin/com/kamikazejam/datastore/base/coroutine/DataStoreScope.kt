package com.kamikazejam.datastore.base.coroutine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

@Suppress("unused")
interface DataStoreScope : CoroutineScope {
    override val coroutineContext: CoroutineContext
    get() = Dispatchers.IO
}