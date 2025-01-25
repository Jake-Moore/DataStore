package com.kamikazejam.datastore.base.data

import com.google.common.base.Preconditions
import com.kamikazejam.datastore.base.data.impl.StoreDataUUID
import org.bson.Document

/**
 * Implementation of StoreData for simple value types that don't contain nested fields
 */
abstract class SimpleStoreData<T : Any>(private var value: T) : StoreData<T>() {
    fun get(): T {
        Preconditions.checkState(
            parent != null,
            "[SimpleStoreData#get] Data not registered with a parent document"
        )
        return value
    }

    fun set(value: T) {
        Preconditions.checkState(isWriteable, "Cannot modify data '${this.javaClass.simpleName}' in read-only mode")
        this.value = value
    }

    /**
     * !! WARNING !!
     *
     * The return value MUST be compatible with MongoDB's BSON serialization! Do not return custom objects or java classes!!
     */
    abstract fun serializeToBSON(): Any

    /**
     * !! WARNING !!
     *
     * Use [key] to obtain the value from the [bson] document.
     */
    abstract fun deserializeFromBSON(bson: Document, key: String)

    final override fun getType(): Companion.Type {
        return Companion.Type.SIMPLE
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StoreDataUUID) return false
        return get() == other.get()
    }

    override fun hashCode(): Int {
        return get().hashCode()
    }
}