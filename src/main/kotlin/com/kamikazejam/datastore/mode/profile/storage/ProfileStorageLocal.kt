package com.kamikazejam.datastore.mode.profile.storage

import com.kamikazejam.datastore.base.storage.StorageLocal
import com.kamikazejam.datastore.mode.store.StoreProfile
import java.util.UUID

class ProfileStorageLocal<X : StoreProfile<X>> : StorageLocal<UUID, X>() {
    override val layerName: String
        get() = "Profile Local"
}
