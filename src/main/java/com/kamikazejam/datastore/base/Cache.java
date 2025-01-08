package com.kamikazejam.datastore.base;

import com.kamikazejam.datastore.DataStoreRegistration;
import com.kamikazejam.datastore.base.cache.StoreLoader;
import com.kamikazejam.datastore.base.exception.DuplicateCacheException;
import com.kamikazejam.datastore.base.index.IndexedField;
import com.kamikazejam.datastore.base.log.LoggerService;
import com.kamikazejam.datastore.base.storage.StorageDatabase;
import com.kamikazejam.datastore.base.storage.StorageLocal;
import com.kamikazejam.datastore.base.storage.StorageMethods;
import com.kamikazejam.datastore.base.store.StoreInstantiator;
import com.kamikazejam.datastore.mode.object.ObjectCache;
import com.kamikazejam.datastore.mode.profile.ProfileCache;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * A Cache holds Store objects and manages their retrieval, caching, and saving.
 * Getters vary by Store type, they are defined in the store-specific interfaces:
 * {@link ObjectCache} and {@link ProfileCache}
 */
@SuppressWarnings({"UnusedReturnValue", "unused"})
public interface Cache<K, X extends Store<K>> extends Service {


    // ------------------------------------------------------ //
    // CRUD Methods                                           //
    // ------------------------------------------------------ //

    // create is in ObjectCache or ProfileCache
    // readOrCreate is in ObjectCache or ProfileCache

    /**
     * Read a Store from this cache (or the database if it doesn't exist in the cache)
     * @param key The key of the Store to read.
     * @return The Store object. (READ-ONLY) (optional)
     */
    @NotNull
    Optional<X> read(@NotNull K key);

    /**
     * Read a Store from this cache (or the database if it doesn't exist in the cache)
     * @param key The key of the Store to read.
     * @param cacheStore If we should cache the Store upon retrieval. (if it was found)
     * @return The Store object. (READ-ONLY) (optional)
     */
    @NotNull
    Optional<X> read(@NotNull K key, boolean cacheStore);

    /**
     * Modifies a Store in a controlled environment where modifications are allowed
     * @throws NoSuchElementException if the Store (by this key) is not found
     * @return The updated Store object. (READ-ONLY)
     */
    X update(@NotNull K key, @NotNull Consumer<X> updateFunction);

    /**
     * Modifies a Store in a controlled environment where modifications are allowed
     * @throws NoSuchElementException if the Store (by this key) is not found
     * @return The updated Store object. (READ-ONLY)
     */
    X update(@NotNull X store, @NotNull Consumer<X> updateFunction);

    /**
     * Deletes a Store by ID (removes from both cache and database)
     */
    void delete(@NotNull K key);
    /**
     * Deletes a Store (removes from both cache and database)
     */
    void delete(@NotNull X store);

    /**
     * Retrieves ALL Stores, including cached values and additional values from database.
     * @param cacheStores If true, any additional Store fetched from db will be cached.
     * @return An Iterable of all Store, for sequential processing. (READ-ONLY)
     */
    @Blocking
    @NotNull
    Iterable<X> readAll(boolean cacheStores);


    // ------------------------------------------------------ //
    // Database Methods                                       //
    // ------------------------------------------------------ //
    /**
     * Retrieves ALL Store IDs from the database.
     * @return An Iterable of all Store Keys, for sequential processing.
     */
    @Blocking
    @NotNull
    Iterable<K> getIDs();

    /**
     * Loads all Stores directly from db, bypassing the cache.
     * Unless you have a reason to use this, please use {@link #readAll(boolean)} instead.
     * @param cacheStores If true, stores loaded from the database will be cached.
     * @return An Iterable of all Stores, for sequential processing. (READ-ONLY)
     */
    @Blocking
    @NotNull
    Iterable<X> readAllFromDatabase(boolean cacheStores);



    // ------------------------------------------------------ //
    // Cache Methods                                          //
    // ------------------------------------------------------ //

    /**
     * Get the name of this cache (set by the end user, should be unique)
     * A {@link DuplicateCacheException} error will be thrown if another cache
     * exists with the same name or ID during creation.
     *
     * @return String: Cache Name
     */
    @NotNull
    String getName();

    /**
     * Retrieve a Store from this cache. (Does not query the database)
     *
     * @return The Store if it was cached.
     */
    @NotNull
    Optional<X> getFromCache(@NotNull K key);

    /**
     * Retrieve a Store from the database. (Force queries the database, and updates this cache)
     *
     * @param cacheStore If we should cache the Store upon retrieval. (if it was found)
     * @return The Store if it was found in the database.
     */
    @NotNull
    Optional<X> getFromDatabase(@NotNull K key, boolean cacheStore);

    /**
     * Adds a Store to this cache.
     */
    void cache(@NotNull X store);

    /**
     * Removes a Store from this cache.
     */
    void uncache(@NotNull K key);

    /**
     * Removes a Store from this cache.
     */
    void uncache(@NotNull X store);

    /**
     * Checks if a Store is in this Cache.
     *
     * @return True if the Store is cached. False if not (for instance if it was deleted)
     */
    boolean isCached(@NotNull K key);

    /**
     * Gets all Store objects that are in this cache.
     */
    @NotNull
    Collection<X> getCached();

    /**
     * Gets the {@link LoggerService} for this cache. For logging purposes.
     */
    @NotNull
    LoggerService getLoggerService();

    /**
     * Sets the {@link LoggerService} for this cache.
     */
    void setLoggerService(@NotNull LoggerService loggerService);

    /**
     * Gets the {@link StorageMethods} that handles local storage for this cache.
     */
    @NotNull
    StorageLocal<K, X> getLocalStore();

    /**
     * Gets the {@link StorageMethods} that handles database storage for this cache.
     */
    @NotNull
    StorageDatabase<K, X> getDatabaseStore();

    /**
     * Gets the plugin that set up this cache.
     */
    @NotNull
    Plugin getPlugin();

    /**
     * Gets the registration the parent plugin used to create this cache.
     */
    @NotNull
    DataStoreRegistration getRegistration();

    /**
     * Return the name of actual the MongoDB database this cache is stored in
     * This is different from the developer supplied db name, and is calculated from
     * {@link com.kamikazejam.datastore.DataStoreAPI#getFullDatabaseName(String)}.
     */
    @NotNull
    String getDatabaseName();

    /**
     * Converts a Cache key to a string. Key uniqueness should be maintained.
     */
    @NotNull
    String keyToString(@NotNull K key);

    /**
     * Converts a string to a Cache key. Key uniqueness should be maintained.
     */
    @NotNull
    K keyFromString(@NotNull String key);

    /**
     * Add a dependency on another Cache. This Cache will be loaded after the dependency.
     */
    void addDepend(@NotNull Cache<?, ?> cache);

    /**
     * Check if this Cache is dependent on the provided cache.
     */
    boolean isDependentOn(@NotNull Cache<?, ?> cache);

    /**
     * Check if this Cache is dependent on the provided cache.
     */
    boolean isDependentOn(@NotNull String cacheName);

    /**
     * Gets the name of all Cache objects this Cache is dependent on.
     */
    @NotNull
    Set<String> getDependencyNames();

    /**
     * Helper method to use the {@link #getPlugin()} plugin to run an async bukkit task.
     */
    @ApiStatus.Internal
    void runAsync(@NotNull Runnable runnable);

    /**
     * Helper method to use the {@link #getPlugin()} plugin to run a sync bukkit task.
     */
    @ApiStatus.Internal
    void runSync(@NotNull Runnable runnable);

    /**
     * Helper method to use the {@link #getPlugin()} plugin to attempt an Async task
     * If the plugin is not allowed to run async tasks (like on disable), a sync task will be run instead.
     */
    @ApiStatus.Internal
    void tryAsync(@NotNull Runnable runnable);

    /**
     * Get the number of Store objects currently stored locally in this cache
     */
    long getLocalCacheSize();

    /**
     * @return True iff the cache contains a Store with the provided key.
     */
    boolean hasKey(@NotNull K key);

    /**
     * Gets the {@link StoreLoader} for the provided key.
     */
    @NotNull
    @ApiStatus.Internal
    StoreLoader<X> loader(@NotNull K key);

    /**
     * Gets the class of the STore object this cache is associated with.
     */
    @ApiStatus.Internal
    @NotNull
    Class<X> getStoreClass();

    /**
     * Returns the StoreInstantiator for the Store object in this cache.
     */
    @NotNull StoreInstantiator<K, X> getInstantiator();

    /**
     * Internal method used by DataStore to forcefully update a local instance of a Store object with a newer one,
     * allowing your references to the existing Store to remain intact and up-to-date.
     * Note that this only effects persistent (non-transient) fields.
     *
     * @param store   The Store to update
     * @param update The newer version of said Store to replace the values of {@param Store} with.
     */
    @ApiStatus.Internal
    void updateStoreFromNewer(@NotNull X store, @NotNull X update);



    // ------------------------------------------------- //
    //                     Indexing                      //
    // ------------------------------------------------- //

    /**
     * Register an index for this cache.
     * @return The registered index (for chaining)
     */
    <T> IndexedField<X, T> registerIndex(@NotNull IndexedField<X, T> field);

    /**
     * Updates the indexes cache with the provided Store object.
     */
    @ApiStatus.Internal
    void cacheIndexes(@NotNull X store, boolean save);

    /**
     * Updates the indexes cache with the provided Store object.
     */
    @ApiStatus.Internal
    void invalidateIndexes(@NotNull K key, boolean save);

    /**
     * Saves the index cache to storage.
     */
    @ApiStatus.Internal
    void saveIndexCache();

    @Nullable
    <T> K getStoreIdByIndex(IndexedField<X, T> index, T value);

    /**
     * Retrieves an object by the provided index field and its value.
     */
    @NotNull
    <T> Optional<X> getByIndex(@NotNull IndexedField<X, T> field, @NotNull T value);
}

