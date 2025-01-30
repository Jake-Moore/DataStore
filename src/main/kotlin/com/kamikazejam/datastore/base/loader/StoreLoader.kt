package com.kamikazejam.datastore.base.loader

import com.kamikazejam.datastore.base.Collection
import com.kamikazejam.datastore.mode.store.Store

/**
 * This class is responsible for loading a [Store] when requested from a [Collection].
 */
interface StoreLoader<X : Store<*, *>> {
    suspend fun fetch(saveToLocalCache: Boolean): X?
}
