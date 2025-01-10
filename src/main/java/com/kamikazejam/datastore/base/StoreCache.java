package com.kamikazejam.datastore.base;

import com.google.common.base.Preconditions;
import com.kamikazejam.datastore.DataStoreAPI;
import com.kamikazejam.datastore.DataStoreRegistration;
import com.kamikazejam.datastore.DataStoreSource;
import com.kamikazejam.datastore.base.exception.DuplicateCacheException;
import com.kamikazejam.datastore.base.field.FieldProvider;
import com.kamikazejam.datastore.base.field.FieldWrapper;
import com.kamikazejam.datastore.base.index.IndexedField;
import com.kamikazejam.datastore.base.log.LoggerService;
import com.kamikazejam.datastore.base.store.CacheLoggerInstantiator;
import com.kamikazejam.datastore.base.store.StoreInstantiator;
import com.kamikazejam.datastore.mode.profile.StoreProfileCache;
import com.kamikazejam.datastore.mode.profile.listener.ProfileListener;
import com.mongodb.DuplicateKeyException;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

/**
 * The abstract backbone of all Store cache systems.
 * All Caching modes (profile, object, simple) extend this class.
 */
@Getter
public abstract class StoreCache<K, X extends Store<X, K>> implements Comparable<StoreCache<?, ?>>, Cache<K, X> {

    protected final Set<String> dependingCaches = new HashSet<>();
    protected final Class<K> keyClass;
    protected final Class<X> storeClass;
    protected final String name;

    protected final DataStoreRegistration registration;
    protected final Plugin plugin;

    protected LoggerService loggerService;
    protected StoreInstantiator<K, X> instantiator;
    protected boolean debug = true;
    protected boolean running = false;

    public StoreCache(StoreInstantiator<K, X> instantiator, String name, Class<K> key, Class<X> storeClass, DataStoreRegistration registration, CacheLoggerInstantiator logger) {
        this.instantiator = instantiator;
        this.name = name;
        this.keyClass = key;
        this.storeClass = storeClass;
        this.registration = registration;
        this.plugin = registration.getPlugin();
        this.loggerService = logger.instantiate(this);
        // Make sure to load the Index Cache from disk when this cache is created
        this.saveIndexCache();
    }

    // ------------------------------------------------------ //
    // Generic CRUD Methods                                   //
    // ------------------------------------------------------ //
    @Override
    public @NotNull Optional<X> read(@NotNull K key, boolean cacheStore) {
        Preconditions.checkNotNull(key, "Key cannot be null");

        // Try Local Cache First
        Optional<X> oLocal = getLocalStore().get(key);
        if (oLocal.isPresent()) {
            final X store = oLocal.get();
            store.setReadOnly(true);
            return Optional.of(store);
        }

        // Try Database Second
        Optional<X> oDatabase = getDatabaseStore().get(key);
        if (oDatabase.isPresent()) {
            final X store = oDatabase.get();
            store.setReadOnly(true);
            if (cacheStore) { cache(store); }
            return Optional.of(store);
        }

        return Optional.empty();
    }

    @Override
    public final @NotNull Optional<X> read(@NotNull K key) {
        return this.read(key, true);
    }

    @Override
    public final @NotNull X readOrCreate(@NotNull K key, @NotNull Consumer<X> initializer) {
        Preconditions.checkNotNull(key, "Key cannot be null");
        Preconditions.checkNotNull(initializer, "Initializer cannot be null");

        Optional<X> o = read(key);
        return o.orElseGet(() -> create(key, initializer));
    }

    @Override
    public final @NotNull X create(@NotNull K key, @NotNull Consumer<X> initializer) throws DuplicateKeyException {
        Preconditions.checkNotNull(key, "Key cannot be null");
        Preconditions.checkNotNull(initializer, "Initializer cannot be null");

        try {
            // Create a new instance in modifiable state
            X store = instantiator.instantiate();
            store.initialize();
            store.setReadOnly(false);

            // Set the id first (allowing the initializer to change it if necessary)
            store.getIdField().set(key);
            // Initialize the store
            initializer.accept(store);
            // Enforce Version 0 for creation
            store.getVersionField().set(0L);

            store.setReadOnly(true);

            // Save the store to our database implementation & cache
            this.cache(store);
            this.getDatabaseStore().save(store);
            return store;
        } catch (DuplicateKeyException d) {
            this.getLoggerService().severe("Failed to create Store: Duplicate Key...");
            throw d;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Store", e);
        }
    }

    @Override
    public final void delete(@NotNull K key) {
        Preconditions.checkNotNull(key, "Key cannot be null");
        getLocalStore().remove(key);
        getDatabaseStore().remove(key);
        this.invalidateIndexes(key, true);
    }

    @Override
    public final void delete(@NotNull X store) {
        Preconditions.checkNotNull(store, "Store cannot be null");
        delete(store.getId());
    }

    @Override
    public X update(@NotNull K key, @NotNull Consumer<X> updateFunction) {
        Preconditions.checkNotNull(key, "key cannot be null");
        Preconditions.checkNotNull(updateFunction, "Update function cannot be null");

        X originalEntity = read(key).orElse(null);
        if (originalEntity == null) {
            throw new NoSuchElementException("[StoreCache#update] Store not found with key: " + key);
        }

        if (!this.getDatabaseStore().update(originalEntity, updateFunction)) {
            throw new IllegalStateException("[StoreCache#update] Failed to update store with key: " + key);
        }
        return originalEntity;
    }

    @Override
    public X update(@NotNull X store, @NotNull Consumer<X> updateFunction) {
        return update(store.getId(), updateFunction);
    }

    // ------------------------------------------------------ //
    // Service Methods                                        //
    // ------------------------------------------------------ //
    /**
     * Start the Cache
     * Should be called by the external plugin during startup after the cache has been created
     *
     * @return Boolean successful
     */
    @Override
    public final boolean start() {
        Preconditions.checkState(!running, "Cache " + name + " is already started!");
        Preconditions.checkNotNull(instantiator, "Instantiator must be set before calling start() for cache " + name);
        boolean success = true;
        if (!initialize()) {
            success = false;
            loggerService.error("Failed to initialize internally for cache: " + name);
        }
        running = true;

        // Register this cache
        try {
            DataStoreAPI.saveCache(this);
        } catch (DuplicateCacheException e) {
            loggerService.severe("[DuplicateCacheException] Failed to register cache: " + name + " - Cache Name already exists!");
            return false;
        }
        return success;
    }

    /**
     * Stop the Cache
     * Should be called by the external plugin during shutdown
     *
     * @return Boolean successful
     */
    public final boolean shutdown() {
        Preconditions.checkState(running, "Cache " + name + " is not running!");
        boolean success = true;

        // If this cache is a player cache, save all profiles of online players before we shut down
        if (this instanceof StoreProfileCache<?> cache) {
            Bukkit.getOnlinePlayers().forEach(p -> ProfileListener.quit(p, cache, false));
        }

        // terminate() handles the rest of the cache shutdown
        if (!terminate()) {
            success = false;
            loggerService.info("Failed to terminate internally for cache: " + name);
        }

        running = false;

        // Unregister this cache
        DataStoreAPI.removeCache(this);
        return success;
    }



    // ------------------------------------------------------ //
    // Cache Methods                                          //
    // ------------------------------------------------------ //
    /**
     * Starts up & initializes the cache.
     * Prepares everything for a fresh startup, ensures database connections, etc.
     */
    @ApiStatus.Internal
    protected abstract boolean initialize();

    /**
     * Shut down the cache.
     * Saves everything first, and safely shuts down
     */
    @ApiStatus.Internal
    protected abstract boolean terminate();

    @NotNull
    @Override
    public final String getName() {
        return name;
    }

    @NotNull
    @Override
    public final Plugin getPlugin() {
        return plugin;
    }

    @NotNull
    @Override
    public DataStoreRegistration getRegistration() {
        return registration;
    }

    @Override
    public @NotNull String getDatabaseName() {
        return registration.getDatabaseName();
    }

    @Override
    public void cache(@NotNull X store) {
        Preconditions.checkNotNull(store);
        Optional<X> o = getLocalStore().get(store.getId());
        if (o.isPresent()) {
            // If the objects are different -> update the one in the cache
            //   Note: this is not an equality check, this is a reference check (as intended)
            if (o.get() != store) {
                updateStoreFromNewer(o.get(), store);
            }
        } else {
            getLocalStore().save(store);
            this.getLoggerService().debug("Cached store " + store.getId());
        }
        store.setCache(this);
    }

    @Override
    public void uncache(@NotNull K key) {
        Preconditions.checkNotNull(key);
        getLocalStore().remove(key);
    }

    @Override
    public void uncache(@NotNull X store) {
        Preconditions.checkNotNull(store);
        getLocalStore().remove(store);
    }

    @Override
    public boolean isCached(@NotNull K key) {
        Preconditions.checkNotNull(key);
        return getLocalStore().has(key);
    }

    @Override
    public void runAsync(@NotNull Runnable runnable) {
        Preconditions.checkNotNull(runnable);
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, runnable);
    }

    @Override
    public void runSync(@NotNull Runnable runnable) {
        Preconditions.checkNotNull(runnable);
        plugin.getServer().getScheduler().runTask(plugin, runnable);
    }

    @Override
    public void tryAsync(@NotNull Runnable runnable) {
        try {
            runAsync(runnable);
        }catch (IllegalPluginAccessException e) {
            runnable.run();
        }
    }

    @Override
    public void addDepend(@NotNull Cache<?, ?> cache) {
        Preconditions.checkNotNull(cache);
        this.dependingCaches.add(cache.getName());
    }

    @Override
    public boolean isDependentOn(@NotNull Cache<?, ?> cache) {
        Preconditions.checkNotNull(cache);
        return dependingCaches.contains(cache.getName());
    }

    @Override
    public boolean isDependentOn(@NotNull String cacheName) {
        Preconditions.checkNotNull(cacheName);
        return dependingCaches.contains(cacheName);
    }

    /**
     * Simple comparator method to determine order between caches based on dependencies
     *
     * @param o The {@link StoreCache} to compare.
     * @return Comparator sorting integer
     */
    @Override
    public int compareTo(@NotNull StoreCache<?, ?> o) {
        Preconditions.checkNotNull(o);
        if (this.isDependentOn(o)) {
            return -1;
        } else if (o.isDependentOn(this)) {
            return 1;
        } else {
            return 0;
        }
    }

    @Override
    public @NotNull Set<String> getDependencyNames() {
        return dependingCaches;
    }

    @Override
    public @NotNull StoreInstantiator<K, X> getInstantiator() {
        return instantiator;
    }

    @Override
    @ApiStatus.Internal
    @SuppressWarnings("unchecked")
    public void updateStoreFromNewer(@NotNull X store, @NotNull X update) {
        Preconditions.checkNotNull(store);
        Preconditions.checkNotNull(update);

        // For this, we will simply go through the store's fields and set them to the update's fields values
        // This is a simple way to update the store from the update/
        // This is likely impossible to do in a fully type-safe way, but assuming both stores are the same type, this should work
        store.setReadOnly(false);
        Map<String, FieldProvider> storeFields = store.getAllFieldsMap();
        Map<String, FieldProvider> updateFields = update.getAllFieldsMap();
        for (Map.Entry<String, FieldProvider> entry : storeFields.entrySet()) {
            FieldProvider storeProvider = entry.getValue();
            FieldProvider updateProvider = updateFields.get(entry.getKey());
            if (updateProvider != null) {
                ((FieldWrapper<Object>) storeProvider.getFieldWrapper()).set(updateProvider.getFieldWrapper().get());
            }
        }

        store.setReadOnly(true);
    }



    // ------------------------------------------------- //
    //                     Indexing                      //
    // ------------------------------------------------- //

    @Override
    public <T> IndexedField<X, T> registerIndex(@NotNull IndexedField<X, T> field) {
        getLoggerService().debug("Registering index: " + field.getName());
        DataStoreSource.getStorageService().registerIndex(this, field);
        return field;
    }

    @Override
    public void cacheIndexes(@NotNull X store, boolean save) {
        DataStoreSource.getStorageService().cacheIndexes(this, store, save);
    }

    @Override
    public void invalidateIndexes(@NotNull K key, boolean save) {
        DataStoreSource.getStorageService().invalidateIndexes(this, key, save);
    }

    @Override
    public void saveIndexCache() {
        DataStoreSource.getStorageService().saveIndexCache(this);
    }

    @Override
    public <T> @Nullable K getStoreIdByIndex(IndexedField<X, T> index, T value) {
        return DataStoreSource.getStorageService().getStoreIdByIndex(this, index, value);
    }

    @Override
    public <T> @NotNull Optional<X> getByIndex(@NotNull IndexedField<X, T> field, @NotNull T value) {
        // 1. -> Check local cache (brute force)
        for (X store : getLocalStore().getAll()) {
            if (field.equals(field.getValue(store), value)) {
                return Optional.of(store);
            }
        }

        // 2. -> Check database (uses cache or mongodb)
        @Nullable K id = DataStoreSource.getStorageService().getStoreIdByIndex(this, field, value);
        if (id == null) {
            return Optional.empty();
        }

        // 3. -> Obtain the Profile by its ID
        Optional<X> o = this.read(id);
        if (o.isPresent() && !field.equals(value, field.getValue(o.get()))) {
            // This can happen if:
            //    The local copy had its field changed
            //    and those changes were not saved to DB or Index Cache
            // This is not considered an error, but we should return empty
            return Optional.empty();
        }

        // Either the Optional is empty or the Store has the correct value -> return
        return o;
    }
}
