package com.kamikazejam.datastore.base.storage;

import com.google.common.base.Preconditions;
import com.kamikazejam.datastore.base.Cache;
import com.kamikazejam.datastore.base.Store;
import com.kamikazejam.datastore.util.DataStoreFileLogger;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Getter
public abstract class StorageLocal<K, X extends Store<X, K>> implements StorageMethods<K, X> {

    private final ConcurrentMap<K, X> localCache = new ConcurrentHashMap<>();

    // ----------------------------------------------------- //
    //                     Store Methods                     //
    // ----------------------------------------------------- //
    @Override
    public Optional<X> get(@NotNull K key) {
        Preconditions.checkNotNull(key);
        return Optional.ofNullable(this.localCache.get(key));
    }

    @Override
    public boolean save(@NotNull X store) {
        // Ensure we don't re-cache an invalid Store
        if (!store.isValid()) {
            DataStoreFileLogger.warn(store.getCache(), "StoreLocal.save(X) w/ Invalid Store. Cache: " + store.getCache().getName() + ", Id: " + store.getId());
            return false;
        }

        // If not called already, call initialized (since we're caching it)
        this.localCache.put(store.getId(), store);
        return true;
    }

    @Override
    public boolean has(@NotNull K key) {
        return this.localCache.containsKey(key);
    }

    @Override
    public boolean has(@NotNull X store) {
        return this.has(store.getId());
    }

    @Override
    public boolean remove(@NotNull K key) {
        @Nullable X x = this.localCache.remove(key);
        if (x != null) {
            x.invalidate();
        }
        return x != null;
    }

    @Override
    public boolean remove(@NotNull X store) {
        return this.remove(store.getId());
    }

    @NotNull
    @Override
    public Iterable<X> getAll() {
        return this.localCache.values();
    }

    @NotNull
    @Override
    public Set<K> getKeys() {
        return this.localCache.keySet();
    }

    @NotNull
    @Override
    public Set<String> getKeyStrings(@NotNull Cache<K, X> cache) {
        return this.localCache.keySet().stream().map(cache::keyToString).collect(Collectors.toSet());
    }

    @Override
    public long clear() {
        final int size = this.localCache.size();
        this.localCache.clear();
        return size;
    }

    @Override
    public long size() {
        return this.localCache.size();
    }

    @Override
    public boolean isDatabase() {
        return false;
    }
}
