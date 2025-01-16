package com.kamikazejam.datastore.mode.`object`.store

import com.kamikazejam.datastore.base.storage.StorageLocal
import com.kamikazejam.datastore.mode.`object`.StoreObject

@Suppress("unused")
class ObjectStorageLocal<X : StoreObject<X>> : StorageLocal<String, X>() {
    override val layerName: String
        get() = "Object Local"
}
