package com.kamikazejam.datastore.mode.object.store;
import com.kamikazejam.datastore.base.Cache;
import com.kamikazejam.datastore.base.storage.StorageDatabase;
import com.kamikazejam.datastore.mode.object.StoreObject;
import org.jetbrains.annotations.NotNull;

public class ObjectStorageDatabase<X extends StoreObject<X>> extends StorageDatabase<String, X> {

    public ObjectStorageDatabase(Cache<String, X> cache) {
        super(cache);
    }

    @Override
    public @NotNull String getLayerName() {
        return "Object MongoDB";
    }
}
