package com.kamikazejam.datastore.base.storage.data;

import com.kamikazejam.datastore.base.Cache;
import com.kamikazejam.datastore.base.Store;
import lombok.Getter;

import java.util.concurrent.CompletableFuture;

@Getter
public class StorageUpdateTask<K, X extends Store<X, K>> {
    private final CompletableFuture<X> finalFuture = new CompletableFuture<>();
    private final Cache<K, X> cache;
    private final X baseCopy;
    private final X originalStore;
    public StorageUpdateTask(Cache<K, X> cache, X baseCopy, X originalStore) {
        this.cache = cache;
        this.baseCopy = baseCopy;
        this.originalStore = originalStore;
    }

    public CompletableFuture<Boolean> completeAsync() {
        return CompletableFuture.supplyAsync(() -> {

        });
    }
}
