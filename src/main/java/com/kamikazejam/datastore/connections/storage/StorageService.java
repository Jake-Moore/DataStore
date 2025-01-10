package com.kamikazejam.datastore.connections.storage;

import com.kamikazejam.datastore.base.Cache;
import com.kamikazejam.datastore.base.Service;
import com.kamikazejam.datastore.base.Store;
import com.kamikazejam.datastore.base.StoreCache;
import com.kamikazejam.datastore.base.index.IndexedField;
import com.kamikazejam.datastore.base.log.LoggerService;
import com.kamikazejam.datastore.base.storage.data.StorageUpdateTask;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NonBlocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Defines the minimum set of methods all Storage services must implement.
 */
@SuppressWarnings({"UnusedReturnValue", "unused"})
public abstract class StorageService extends LoggerService implements Service {

    /**
     * Save a Store to this store. Requires the cache it belongs to.
     * Implementations of this class should handle optimistic versioning and throw errors accordingly.
     * @return If the Store was saved.
     */
    public abstract <K, X extends Store<X, K>> boolean save(Cache<K, X> cache, X store);

    /**
     * Replace a Store in this database. This requires that we can find the Store in the database.<br>
     * If found, then the document in the database is replaced using a transaction. (providing atomicity)
     * @return If the Store was replaced. (if the db was updated)
     */
    @Blocking
    public abstract <K, X extends Store<X, K>> boolean updateSync(Cache<K, X> cache, X store, @NotNull Consumer<X> updateFunction);

    /**
     * Replace a Store in this database. This requires that we can find the Store in the database.<br>
     * If found, then the document in the database is replaced using a transaction. (providing atomicity)
     * @return If the Store was replaced. (if the db was updated)
     */
    @NonBlocking
    public abstract <K, X extends Store<X, K>> @NotNull StorageUpdateTask<K, X> update(Cache<K, X> cache, X store, @NotNull Consumer<X> updateFunction);

    /**
     * Retrieve a Store from this store. Requires the cache to fetch it from.
     */
    @NotNull
    public abstract <K, X extends Store<X, K>> Optional<X> get(Cache<K, X> cache, K key);

    /**
     * @return How many Stores are stored in a cache within this store.
     */
    public abstract <K, X extends Store<X, K>> long size(Cache<K, X> cache);

    /**
     * Check if a Store is stored in a given cache.
     */
    public abstract <K, X extends Store<X, K>> boolean has(Cache<K, X> cache, K key);

    /**
     * Remove a Store from a given cache.
     */
    public abstract <K, X extends Store<X, K>> boolean remove(Cache<K, X> cache, K key);

    /**
     * Retrieve all Stores from a specific cache.
     */
    public abstract <K, X extends Store<X, K>> Iterable<X> getAll(Cache<K, X> cache);

    /**
     * Retrieve all Store keys from a specific cache.
     */
    public abstract <K, X extends Store<X, K>> Iterable<K> getKeys(Cache<K, X> cache);

    /**
     * @return If the StorageService is ready to be used for a cache.
     */
    public abstract boolean canCache();

    /**
     * Called when a cache is registered with the StoreEngine -> meant for internal initialization.
     */
    public abstract <K, X extends Store<X, K>> void onRegisteredCache(Cache<K, X> cache);

    /**
     * Test the ping to the storage service. Will block thread until ping is calculated.
     * @return The ping (in Nanoseconds) to the storage service. (Returns -1 if ping failed or is not available)
     */
    @Blocking
    public abstract long getPingNano();

    /**
     * Get the average ping to the storage service (round trip). (cached value from last heartbeat)
     */
    public abstract long getAveragePingNanos();

    // ------------------------------------------------- //
    //                     Indexing                      //
    // ------------------------------------------------- //
    public abstract <K, X extends Store<X, K>, T> void registerIndex(@NotNull StoreCache<K, X> cache, IndexedField<X, T> index);
    public abstract <K, X extends Store<X, K>> void cacheIndexes(@NotNull StoreCache<K, X> cache, @NotNull X store, boolean updateFile);
    public abstract <K, X extends Store<X, K>> void saveIndexCache(@NotNull StoreCache<K, X> cache);
    public abstract <K, X extends Store<X, K>, T> @Nullable K getStoreIdByIndex(@NotNull StoreCache<K, X> cache, IndexedField<X, T> index, T value);
    public abstract <K, X extends Store<X, K>> void invalidateIndexes(@NotNull StoreCache<K, X> cache, @NotNull K key, boolean updateFile);

}
