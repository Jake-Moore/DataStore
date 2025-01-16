package com.kamikazejam.datastore.base.index

import com.kamikazejam.datastore.base.Cache
import com.kamikazejam.datastore.base.Store

/**
 * All IndexFields are assumed to be unique (only have one Store with that value)
 */
@Suppress("unused")
abstract class IndexedField<X : Store<X, *>, T>(
    private val cache: Cache<*, X>,
    val name: String
) {
    abstract fun equals(a: T?, b: T?): Boolean

    abstract fun <K, Y : Store<Y, K>> getValue(store: Y): T

    abstract fun toString(value: Any): String

    abstract fun fromString(value: String): T
}
