package com.kamikazejam.datastore.mode.`object`.store

import com.kamikazejam.datastore.base.Collection
import com.kamikazejam.datastore.base.storage.StorageDatabase
import com.kamikazejam.datastore.mode.`object`.StoreObject

@Suppress("unused")
class ObjectStorageDatabase<X : StoreObject<X>>(collection: Collection<String, X>) :
    StorageDatabase<String, X>(collection) {
    override val layerName: String
        get() = "Object MongoDB"
}
