package com.kamikazejam.datastore.base.cache

import com.kamikazejam.datastore.base.Cache
import com.kamikazejam.datastore.base.Store

/**
 * This class is responsible for loading a [Store] when requested from a [Cache].
 */
interface StoreLoader<X : Store<*, *>?> {
    fun fetch(saveToLocalCache: Boolean): X?
}
