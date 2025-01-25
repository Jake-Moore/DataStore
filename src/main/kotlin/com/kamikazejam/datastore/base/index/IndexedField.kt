package com.kamikazejam.datastore.base.index

import com.kamikazejam.datastore.base.Collection
import com.kamikazejam.datastore.base.Store
import com.kamikazejam.datastore.base.data.StoreData

/**
 * All IndexFields are assumed to be unique (only have one Store with that value)
 */
@Suppress("unused")
abstract class IndexedField<X : Store<X, *>, D: StoreData<Any>>(
    private val collection: Collection<*, X>,
    val name: String
) {
    abstract fun equals(a: D?, b: D?): Boolean

    abstract fun getValue(store: X): D?
}
