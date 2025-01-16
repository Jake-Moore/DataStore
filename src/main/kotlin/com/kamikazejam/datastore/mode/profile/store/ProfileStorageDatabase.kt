package com.kamikazejam.datastore.mode.profile.store

import com.kamikazejam.datastore.base.Cache
import com.kamikazejam.datastore.base.storage.StorageDatabase
import com.kamikazejam.datastore.mode.profile.StoreProfile
import java.util.*

class ProfileStorageDatabase<X : StoreProfile<X>>(cache: Cache<UUID, X>) :
    StorageDatabase<UUID, X>(cache) {
    override val layerName: String
        get() = "Profile MongoDB"
}
