package com.kamikazejam.datastore.base.mode;

import com.google.common.base.Preconditions;
import com.kamikazejam.datastore.DataStoreSource;
import com.kamikazejam.datastore.connections.storage.StorageService;
import com.kamikazejam.datastore.connections.storage.mongo.MongoStorage;
import com.kamikazejam.datastore.util.Color;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

// Abstraction layer for adding different storage modes in the future
public enum StorageMode {
    MONGODB;

    public void enableServices() {
        // Enable Storage Service
        this.getStorageService();
    }

    public @NotNull StorageService getStorageService() {
        return switch (this) {
            case MONGODB -> getMongoStorage();
        };
    }


    // ---------------------------------------------------------------------------- //
    //                         STORAGE SERVICE MANAGEMENT                           //
    // ---------------------------------------------------------------------------- //

    private MongoStorage mongoStorage = null;
    private @NotNull MongoStorage getMongoStorage() {
        Preconditions.checkState(this == StorageMode.MONGODB, "MongoStorage is only available in MONGODB storage mode");
        if (mongoStorage == null) {
            mongoStorage = new MongoStorage();
            if (!mongoStorage.start()) {
                DataStoreSource.get().getLogger().severe(Color.t("&cFailed to start MongoStorage, shutting down..."));
                Bukkit.shutdown();
            }
        }
        return Objects.requireNonNull(mongoStorage);
    }

    public void disableServices() {
        if (mongoStorage != null) {
            mongoStorage.shutdown();
            mongoStorage = null;
        }
    }
}
