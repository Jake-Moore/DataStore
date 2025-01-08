package com.kamikazejam.datastore.mode.profile.store;

import com.kamikazejam.datastore.base.Cache;
import com.kamikazejam.datastore.base.storage.StorageDatabase;
import com.kamikazejam.datastore.mode.profile.StoreProfile;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class ProfileStorageDatabase<X extends StoreProfile> extends StorageDatabase<UUID, X> {

    public ProfileStorageDatabase(Cache<UUID, X> cache) {
        super(cache);
    }

    @Override
    public @NotNull String getLayerName() {
        return "Profile MongoDB";
    }
}
