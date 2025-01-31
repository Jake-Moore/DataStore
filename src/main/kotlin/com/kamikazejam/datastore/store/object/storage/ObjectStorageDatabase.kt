package com.kamikazejam.datastore.store.`object`.storage

import com.kamikazejam.datastore.base.Collection
import com.kamikazejam.datastore.base.storage.StorageDatabase
import com.kamikazejam.datastore.store.StoreObject

@Suppress("unused")
class ObjectStorageDatabase<X : StoreObject<X>>(collection: Collection<String, X>) :
    StorageDatabase<String, X>(collection) {
    override val layerName: String
        get() = "Object MongoDB"
}
