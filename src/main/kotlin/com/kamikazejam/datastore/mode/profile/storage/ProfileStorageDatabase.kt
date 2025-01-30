package com.kamikazejam.datastore.mode.profile.storage

import com.kamikazejam.datastore.base.Collection
import com.kamikazejam.datastore.base.storage.StorageDatabase
import com.kamikazejam.datastore.mode.store.StoreProfile
import java.util.UUID

class ProfileStorageDatabase<X : StoreProfile<X>>(collection: Collection<UUID, X>) :
    StorageDatabase<UUID, X>(collection) {
    override val layerName: String
        get() = "Profile MongoDB"
}
