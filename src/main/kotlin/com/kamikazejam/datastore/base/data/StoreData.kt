package com.kamikazejam.datastore.base.data

import com.google.common.base.Preconditions
import com.kamikazejam.datastore.base.Store

/**
 * StoreData must contain a nonnull value, however at the field level, the field will return StoreData<T>?
 *
 * This allows null values, where internally they are handled as null StoreData, not a null internal value.
 */
@Suppress("MemberVisibilityCanBePrivate")
sealed class StoreData<T : Any>(protected var parent: Store<*, *>? = null) {

    val isWriteable: Boolean
        get() {
            val p = this.parent
            Preconditions.checkState(
                p != null,
                "[StoreData#isWriteable] Field not registered with a parent document"
            )
            checkNotNull(p)
            return !p.readOnly
        }

    abstract override fun equals(other: Any?): Boolean

    abstract override fun hashCode(): Int
}