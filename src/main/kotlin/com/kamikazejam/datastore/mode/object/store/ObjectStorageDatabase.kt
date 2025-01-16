package com.kamikazejam.datastore.mode.`object`.store

import com.kamikazejam.datastore.base.Cache
import com.kamikazejam.datastore.base.storage.StorageDatabase
import com.kamikazejam.datastore.mode.`object`.StoreObject

@Suppress("unused")
class ObjectStorageDatabase<X : StoreObject<X>>(cache: Cache<String, X>) :
    StorageDatabase<String, X>(cache) {
    override val layerName: String
        get() = "Object MongoDB"
}
