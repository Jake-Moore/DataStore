package com.kamikazejam.datastore.base.data.impl

import com.kamikazejam.datastore.base.data.SimpleStoreData
import org.bson.Document
import java.util.*

class StoreDataUUID(uuid: UUID) : SimpleStoreData<UUID>(value = uuid) {
    override fun serializeToBSON(): Any {
        return get().toString()
    }

    override fun deserializeFromBSON(bson: Document, key: String) {
        this.set(UUID.fromString(bson.getString(key)))
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