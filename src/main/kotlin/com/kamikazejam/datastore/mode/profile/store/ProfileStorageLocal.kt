package com.kamikazejam.datastore.mode.profile.store

import com.kamikazejam.datastore.base.storage.StorageLocal
import com.kamikazejam.datastore.mode.profile.StoreProfile
import java.util.*

class ProfileStorageLocal<X : StoreProfile<X>> : StorageLocal<UUID, X>() {
    override val layerName: String
        get() = "Profile Local"
}
