package com.kamikazejam.datastore.base.data.impl

import com.kamikazejam.datastore.base.data.SimpleStoreData
import org.bson.Document

class StoreDataString(string: String) : SimpleStoreData<String>(value = string) {
    override fun serializeToBSON(): Any {
        return get()
    }

    override fun deserializeFromBSON(bson: Document, key: String) {
        this.set(bson.getString(key))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StoreDataString) return false
        return get() == other.get()
    }

    override fun hashCode(): Int {
        return get().hashCode()
    }
}