package com.kamikazejam.datastore.mode.object.store;
import com.kamikazejam.datastore.base.storage.StorageLocal;
import com.kamikazejam.datastore.mode.object.StoreObject;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public class ObjectStorageLocal<X extends StoreObject> extends StorageLocal<String, X> {
    public ObjectStorageLocal() {}

    @Override
    public @NotNull String getLayerName() {
        return "Object Local";
    }
}
