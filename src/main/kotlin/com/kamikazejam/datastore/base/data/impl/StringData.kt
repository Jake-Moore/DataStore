package com.kamikazejam.datastore.base.data.impl

import com.kamikazejam.datastore.base.data.SimpleStoreData
import org.bson.Document

class StringData(val string: String) : SimpleStoreData<String>(dataType = String::class.java, value = string) {
    override fun serializeToBSON(): Any {
        return string
    }

    override fun deserializeFromBSON(bson: Document, key: String): String {
        return bson.getString(key)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StringData) return false
        return string == other.string
    }

    override fun hashCode(): Int {
        return string.hashCode()
    }
}