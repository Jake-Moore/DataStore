package com.kamikazejam.datastore.base.storage;

import com.kamikazejam.datastore.base.Cache;
import com.kamikazejam.datastore.base.Store;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Methods that all storage layers must implement.
 */
@SuppressWarnings({"UnusedReturnValue", "unused"})
public interface StorageMethods<K, X extends Store<X, K>> {

    /**
     * Retrieve a Store from this store.
     */
    Optional<X> get(@NotNull K key);

    /**
     * Save a Store to this store.
     * @return If the Store was saved.
     */
    boolean save(@NotNull X store);

    /**
     * Check if a Store is stored in this store.
     */
    boolean has(@NotNull K key);

    /**
     * Check if a Store is stored in this store.
     */
    boolean has(@NotNull X store);

    /**
     * Remove a Store from this store.
     *
     * @return If the Store existed, and was removed.
     */
    boolean remove(@NotNull K key);

    /**
     * Remove a Store from this store.
     *
     * @return If the Store existed, and was removed.
     */
    boolean remove(@NotNull X store);

    /**
     * Retrieve all Stores from this store.
     */
    @NotNull
    Iterable<X> getAll();

    /**
     * Retrieve all Store keys from this store.
     */
    @NotNull
    Iterable<K> getKeys();

    /**
     * Retrieve all Store keys (in string form) from this store.
     * Uses {@link Cache#keyToString(Object)} to convert keys to strings.
     */
    @NotNull
    Iterable<String> getKeyStrings(@NotNull Cache<K, X> cache);

    /**
     * Clear all Stores from this store. No Stores are deleted, just removed from memory.
     */
    long clear();

    /**
     * Gets the name of this storage layer.
     */
    @NotNull
    String getLayerName();

    /**
     * @return How many objects are in this Store
     */
    long size();

    /**
     * @return True IFF this Store is a database (stores data elsewhere)
     */
    @ApiStatus.Internal
    boolean isDatabase();

}
