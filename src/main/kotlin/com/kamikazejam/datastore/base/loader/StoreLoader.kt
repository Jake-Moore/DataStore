package com.kamikazejam.datastore.base.loader

import com.kamikazejam.datastore.base.Collection
import com.kamikazejam.datastore.base.Store

/**
 * This class is responsible for loading a [Store] when requested from a [Collection].
 */
interface StoreLoader<X : Store<*, *>?> {
    fun fetch(saveToLocalCache: Boolean): X?
}
