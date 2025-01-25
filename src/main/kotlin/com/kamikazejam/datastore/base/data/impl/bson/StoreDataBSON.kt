@file:Suppress("unused")

package com.kamikazejam.datastore.base.data.impl.bson

import com.kamikazejam.datastore.base.data.SimpleStoreData

// TODO, we should be able to read (get) from anything even if there is a null parent (because reading is safe and does not mutate)
//  HOWEVER, we should NOT be able to write without a parent, because if we write with a null parent, we will lose the data

sealed class StoreDataBSON<T : Any>(value: T) : SimpleStoreData<T>(value = value) {
    override fun serializeToBSON(): Any {
        // The classes permitted to extend this StoreDataBSON class are expected to be BSON compliant
        // meaning that returning their object, should be compliant when placed in a BSON Document
        return get()
    }
}

class StoreDataDouble(double: Double) : StoreDataBSON<Double>(value = double) {
    override fun deserializeFromBSON(bson: org.bson.Document, key: String) {
        this.set(bson.getDouble(key))
    }
}

class StoreDataLong(long: Long) : StoreDataBSON<Long>(value = long) {
    override fun deserializeFromBSON(bson: org.bson.Document, key: String) {
        this.set(bson.getLong(key))
    }
}

class StoreDataString(string: String) : StoreDataBSON<String>(value = string) {
    override fun deserializeFromBSON(bson: org.bson.Document, key: String) {
        this.set(bson.getString(key))
    }
}

class StoreDataBoolean(boolean: Boolean) : StoreDataBSON<Boolean>(value = boolean) {
    override fun deserializeFromBSON(bson: org.bson.Document, key: String) {
        this.set(bson.getBoolean(key))
    }
}

class StoreDataDate(date: java.util.Date) : StoreDataBSON<java.util.Date>(value = date) {
    override fun deserializeFromBSON(bson: org.bson.Document, key: String) {
        this.set(bson.getDate(key))
    }
}

class StoreDataInt(int: Int) : StoreDataBSON<Int>(value = int) {
    override fun deserializeFromBSON(bson: org.bson.Document, key: String) {
        this.set(bson.getInteger(key))
    }
}

class StoreDataObjectID(objectID: org.bson.types.ObjectId) : StoreDataBSON<org.bson.types.ObjectId>(value = objectID) {
    override fun deserializeFromBSON(bson: org.bson.Document, key: String) {
        this.set(bson.getObjectId(key))
    }
}