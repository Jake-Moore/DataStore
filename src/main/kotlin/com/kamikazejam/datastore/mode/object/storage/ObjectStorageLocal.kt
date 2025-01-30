package com.kamikazejam.datastore.mode.`object`.storage

import com.kamikazejam.datastore.base.storage.StorageLocal
import com.kamikazejam.datastore.mode.store.StoreObject

@Suppress("unused")
class ObjectStorageLocal<X : StoreObject<X>> : StorageLocal<String, X>() {
    override val layerName: String
        get() = "Object Local"
}
