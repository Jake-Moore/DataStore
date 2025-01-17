package com.kamikazejam.datastore.mode.profile.store

import com.kamikazejam.datastore.base.Collection
import com.kamikazejam.datastore.base.storage.StorageDatabase
import com.kamikazejam.datastore.mode.profile.StoreProfile
import java.util.*

class ProfileStorageDatabase<X : StoreProfile<X>>(collection: Collection<UUID, X>) :
    StorageDatabase<UUID, X>(collection) {
    override val layerName: String
        get() = "Profile MongoDB"
}
