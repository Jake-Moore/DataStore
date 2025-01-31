package com.kamikazejam.datastore.store.profile.storage

import com.kamikazejam.datastore.base.storage.StorageLocal
import com.kamikazejam.datastore.store.StoreProfile
import java.util.UUID

class ProfileStorageLocal<X : StoreProfile<X>> : StorageLocal<UUID, X>() {
    override val layerName: String
        get() = "Profile Local"
}
