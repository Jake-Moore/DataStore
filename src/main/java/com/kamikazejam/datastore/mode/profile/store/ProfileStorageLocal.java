package com.kamikazejam.datastore.mode.profile.store;

import com.kamikazejam.datastore.base.storage.StorageLocal;
import com.kamikazejam.datastore.mode.profile.StoreProfile;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class ProfileStorageLocal<X extends StoreProfile> extends StorageLocal<UUID, X> {
    public ProfileStorageLocal() {}

    @Override
    public @NotNull String getLayerName() {
        return "Profile Local";
    }
}
