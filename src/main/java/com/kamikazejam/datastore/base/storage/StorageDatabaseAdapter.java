package com.kamikazejam.datastore.base.storage;

import com.kamikazejam.datastore.base.Cache;
import com.kamikazejam.datastore.base.Store;
import com.kamikazejam.datastore.base.storage.data.StorageUpdateTask;
import lombok.Getter;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NonBlocking;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Adapts the cache-based api in {@link StorageMethods} to produce the simple api of {@link StorageDatabase}.
 */
@Getter
public abstract class StorageDatabaseAdapter<K, X extends Store<X, K>> implements StorageMethods<K, X> {

    protected final Cache<K, X> cache;

    public StorageDatabaseAdapter(Cache<K, X> objectCache) {
        this.cache = objectCache;
    }

    // ---------------------------------------------------------------- //
    //                     Abstraction Conversion                       //
    // ---------------------------------------------------------------- //
    protected abstract Optional<X> get(Cache<K, X> cache, @NotNull K key);

    protected abstract boolean save(Cache<K, X> cache, @NotNull X store);

    @Blocking
    protected abstract boolean updateSync(Cache<K, X> cache, @NotNull X store, @NotNull Consumer<X> updateFunction);

    @NonBlocking
    protected abstract @NotNull StorageUpdateTask<K, X> update(Cache<K, X> cache, @NotNull X store, @NotNull Consumer<X> updateFunction);

    protected abstract boolean has(Cache<K, X> cache, @NotNull K key);

    protected abstract boolean remove(Cache<K, X> cache, @NotNull K key);

    @NotNull
    protected abstract Iterable<X> getAll(Cache<K, X> cache);

    protected abstract long size(Cache<K, X> cache);


    // ---------------------------------------------------------------- //
    //                           StoreMethods                           //
    // ---------------------------------------------------------------- //
    @Override
    public Optional<X> get(@NotNull K key) {
        return this.get(this.cache, key);
    }

    @Override
    public boolean save(@NotNull X store) {
        return this.save(this.cache, store);
    }

    /**
     * Replace a Store in this database. This requires that we can find the Store in the database.<br>
     * If found, then the document in the database is replaced using a transaction. (providing atomicity)
     * @param updateFunction The function to update the Store with.
     * @return If the Store was replaced. (if the db was updated)
     */
    public boolean updateSync(@NotNull X store, @NotNull Consumer<X> updateFunction) {
        return this.updateSync(this.cache, store, updateFunction);
    }

    /**
     * Replace a Store in this database. This requires that we can find the Store in the database.<br>
     * If found, then the document in the database is replaced using a transaction. (providing atomicity)
     * @param updateFunction The function to update the Store with.
     * @return If the Store was replaced. (if the db was updated)
     */
    public @NotNull StorageUpdateTask<K, X> update(@NotNull X store, @NotNull Consumer<X> updateFunction) {
        return this.update(this.cache, store, updateFunction);
    }

    @Override
    public boolean has(@NotNull K key) {
        return this.has(this.cache, key);
    }

    @Override
    public boolean has(@NotNull X store) {
        return this.has(this.cache, store.getId());
    }

    @Override
    public boolean remove(@NotNull K key) {
        return this.remove(this.cache, key);
    }

    @Override
    public boolean remove(@NotNull X store) {
        return this.remove(this.cache, store.getId());
    }

    @Override
    public @NotNull Iterable<X> getAll() {
        return this.getAll(this.cache);
    }

    @Override
    public long size() {
        return this.size(this.cache);
    }

}
