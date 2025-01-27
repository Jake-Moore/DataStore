package com.kamikazejam.datastore.base.data

import com.google.common.base.Preconditions
import com.kamikazejam.datastore.base.Store

/**
 * StoreData must contain a nonnull value, however at the field level, the field will return StoreData<T>?
 *
 * This allows null values, where internally they are handled as null StoreData, not a null internal value.
 */
@Suppress("MemberVisibilityCanBePrivate")
sealed class StoreData<out T : Any> {
    var parent: Store<*, *>? = null
        private set

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

    open fun setParent(parent: Store<*, *>?) {
        // Ensure the passed parent is not null
        Preconditions.checkArgument(parent != null, "Parent cannot be null")
        this.parent = parent
    }

    abstract override fun equals(other: Any?): Boolean

    abstract override fun hashCode(): Int

    abstract fun getType(): Type

    companion object {
        const val TYPE_KEY = "type"
        const val CONTENT_KEY = "content"

        enum class Type {
            SIMPLE,
            COMPOSITE,
        }
    }
}