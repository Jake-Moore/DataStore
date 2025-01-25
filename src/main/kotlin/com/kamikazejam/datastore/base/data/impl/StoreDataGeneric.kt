package com.kamikazejam.datastore.base.data.impl

import com.kamikazejam.datastore.base.data.SimpleStoreData
import com.kamikazejam.datastore.util.JacksonUtil
import org.bson.Document

@Suppress("unused")
class StoreDataGeneric<T : Any>(private var value: T) : SimpleStoreData<T>(value = value) {
    override fun serializeToBSON(): Any {
        return JacksonUtil.objectMapper.writeValueAsString(value)
    }

    override fun deserializeFromBSON(bson: Document, key: String) {
        val value = bson[key] as String
        this.value = JacksonUtil.objectMapper.readValue(value, this.value::class.java)
    }
}