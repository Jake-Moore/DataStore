package com.kamikazejam.datastore.base.index

import com.kamikazejam.datastore.base.Collection
import com.kamikazejam.datastore.base.serialization.SerializationUtil
import com.kamikazejam.datastore.store.Store
import kotlin.reflect.KProperty

/**
 * All IndexFields are assumed to be unique (only have one Store with that value)
 */
@Suppress("unused")
abstract class IndexedField<X : Store<X, *>, T>(
    private val collection: Collection<*, X>,
    private val property: KProperty<*>
) {
    val name: String
        get() = SerializationUtil.getSerialName(collection.getKSerializer(), property)

    abstract fun equals(a: T?, b: T?): Boolean

    abstract fun getValue(store: X): T?
}
