package com.kamikazejam.datastore.base.data.impl

import com.kamikazejam.datastore.base.data.SimpleStoreData
import org.bson.Document

class StoreDataLong(long: Long) : SimpleStoreData<Long>(value = long) {
    override fun serializeToBSON(): Any {
        return get()
    }

    override fun deserializeFromBSON(bson: Document, key: String) {
        this.set(bson.getLong(key))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StoreDataLong) return false
        return get() == other.get()
    }

    override fun hashCode(): Int {
        return get().hashCode()
    }
}