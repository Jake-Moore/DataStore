package com.kamikazejam.datastore.base.storage;

import com.kamikazejam.datastore.DataStoreSource;
import com.kamikazejam.datastore.base.Cache;
import com.kamikazejam.datastore.base.Store;
import com.kamikazejam.datastore.base.storage.data.StorageUpdateTask;
import com.kamikazejam.datastore.connections.storage.StorageService;
import com.kamikazejam.datastore.connections.storage.iterator.TransformingIterator;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Wraps up the StorageService with the Cache backing the Stores, and exposes ObjectStore methods
 *
 * @param <X>
 */
public abstract class StorageDatabase<K, X extends Store<X, K>> extends StorageDatabaseAdapter<K, X> {

    private final StorageService storageService;

    public StorageDatabase(Cache<K, X> cache) {
        super(cache);
        this.storageService = DataStoreSource.getStorageService();
    }


    // ----------------------------------------------------- //
    //                     Store Methods                     //
    // ----------------------------------------------------- //
    @Override
    public long clear() {
        // For safety reasons...
        throw new UnsupportedOperationException("Cannot clear a MongoDB database from within DataStore.");
    }

    @Override
    public boolean isDatabase() {
        return true;
    }


    // ---------------------------------------------------------------- //
    //               Map StoreStorage to StorageService                 //
    // ---------------------------------------------------------------- //
    @Override
    protected Optional<X> get(Cache<K, X> cache, @NotNull K key) {
        // Fetch the Store from the database
        Optional<X> o = storageService.get(cache, key);
        // Save Copy for VersionMismatchException handling
        o.ifPresent(s -> s.setCache(cache));
        // Return the Store
        return o;
    }

    @Override
    protected boolean save(Cache<K, X> cache, @NotNull X store) {
        // All saves to Database Storage run through here
        return storageService.save(cache, store);
    }

    @Override
    protected boolean updateSync(Cache<K, X> cache, @NotNull X store, @NotNull Consumer<X> updateFunction) {
        return storageService.updateSync(cache, store, updateFunction);
    }

    @Override
    protected @NotNull StorageUpdateTask<K, X> update(Cache<K, X> cache, @NotNull X store, @NotNull Consumer<X> updateFunction) {
        return storageService.update(cache, store, updateFunction);
    }

    @Override
    protected boolean has(Cache<K, X> cache, @NotNull K key) {
        return storageService.has(cache, key);
    }

    @Override
    protected boolean remove(Cache<K, X> cache, @NotNull K key) {
        return storageService.remove(cache, key);
    }

    @Override
    protected @NotNull Iterable<X> getAll(Cache<K, X> cache) {
        // Fetch the storageService's Iterable
        Iterator<X> storage = storageService.getAll(cache).iterator();
        return () -> new TransformingIterator<>(storage, x -> {
            // Make sure to set the cache and cacheCopy as we load the Stores
            x.setCache(cache);
            return x;
        });
    }

    @Override
    public @NotNull Iterable<K> getKeys() {
        return storageService.getKeys(cache);
    }

    @NotNull
    @Override
    public Iterable<String> getKeyStrings(@NotNull Cache<K, X> cache) {
        Iterator<K> keys = storageService.getKeys(cache).iterator();
        return () -> new TransformingIterator<>(keys, cache::keyToString);
    }

    @Override
    protected long size(Cache<K, X> cache) {
        return storageService.size(cache);
    }
}
