package com.kamikazejam.datastore.mode.object;

import com.google.common.base.Preconditions;
import com.kamikazejam.datastore.DataStoreRegistration;
import com.kamikazejam.datastore.DataStoreSource;
import com.kamikazejam.datastore.base.StoreCache;
import com.kamikazejam.datastore.base.log.CacheLoggerService;
import com.kamikazejam.datastore.base.log.LoggerService;
import com.kamikazejam.datastore.base.storage.StorageDatabase;
import com.kamikazejam.datastore.base.store.CacheLoggerInstantiator;
import com.kamikazejam.datastore.base.store.StoreInstantiator;
import com.kamikazejam.datastore.connections.storage.iterator.TransformingIterator;
import com.kamikazejam.datastore.mode.object.store.ObjectStorageDatabase;
import com.kamikazejam.datastore.mode.object.store.ObjectStorageLocal;
import com.mongodb.DuplicateKeyException;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

@Getter
@SuppressWarnings("unused")
public abstract class StoreObjectCache<X extends StoreObject<X>> extends StoreCache<String, X> implements ObjectCache<X> {
    private final ConcurrentMap<String, StoreObjectLoader<X>> loaders = new ConcurrentHashMap<>();
    private final ObjectStorageLocal<X> localStore = new ObjectStorageLocal<>();
    private final ObjectStorageDatabase<X> databaseStore = new ObjectStorageDatabase<>(this);

    public StoreObjectCache(DataStoreRegistration module, StoreInstantiator<String, X> instantiator, String name, Class<X> storeClass) {
        // Optional Constructor that will use the default CacheLoggerService
        this(module, instantiator, name, storeClass, CacheLoggerService::new);
    }
    public StoreObjectCache(DataStoreRegistration module, StoreInstantiator<String, X> instantiator, String name, Class<X> storeClass, CacheLoggerInstantiator logger) {
        super(instantiator, name, String.class, storeClass, module, logger);

        // Start this cache
        if (!start()) {
            // Data loss is not tolerated in DataStore, shutdown to prevent issues
            DataStoreSource.get().getLogger().severe("Failed to start Object Cache: " + name);
            Bukkit.shutdown();
        }
    }

    // ------------------------------------------------------ //
    // CRUD Methods                                           //
    // ------------------------------------------------------ //
    @Override
    public final @NotNull X createSync(@NotNull Consumer<X> initializer) throws DuplicateKeyException {
        return this.createSync(UUID.randomUUID().toString(), initializer);
    }

    // ------------------------------------------------------ //
    // Cache Methods                                          //
    // ------------------------------------------------------ //
    @Override
    protected boolean initialize() {
        // Nothing to do here
        return true;
    }

    @Override
    protected boolean terminate() {
        // Don't save -> Stores are updated in their cache's update methods, they should not need to be saved here
        boolean success = true;

        loaders.clear();
        // Clear local store (frees memory)
        localStore.clear();
        // Don't clear database (can't)

        return success;
    }

    @NotNull
    @Override
    public StoreObjectLoader<X> loader(@NotNull String key) {
        Preconditions.checkNotNull(key);
        return loaders.computeIfAbsent(key, s -> new StoreObjectLoader<>(this, s));
    }

    @NotNull
    @Override
    public StorageDatabase<String, X> getDatabaseStore() {
        return databaseStore;
    }

    @Override
    public @NotNull String keyToString(@NotNull String key) {
        return key;
    }

    @Override
    public @NotNull String keyFromString(@NotNull String key) {
        return key;
    }

    @NotNull
    @Override
    public Iterable<X> readAll(boolean cacheStores) {
        Iterator<String> keysIterable = databaseStore.getKeys().iterator();

        // Create an Iterable that iterates through all database keys, transforming into Stores
        return () -> new TransformingIterator<>(keysIterable, key -> {
            // 1. If we have the object in the cache -> return it
            Optional<X> o = localStore.get(key);
            if (o.isPresent()) {
                return o.get();
            }

            // 2. We don't have the object in the cache -> load it from the database
            // If for some reason this was deleted, or not found, we can just return null
            //  and the TransformingIterator will skip it
            Optional<X> db = databaseStore.get(key);
            if (db.isEmpty()) {
                return null;
            }

            // Optionally cache this loaded Store
            if (cacheStores) {
                StoreObjectCache.this.cache(db.get());
            }
            return db.get();
        });
    }

    @Override
    public @NotNull Iterable<X> readAllFromDatabase(boolean cacheStores) {
        // Create an Iterable that iterates through all database objects, and updates local objects as necessary
        Iterator<X> dbIterator = databaseStore.getAll().iterator();
        return () -> new TransformingIterator<>(dbIterator, dbStore -> {
            // Load the local object
            Optional<X> local = localStore.get(dbStore.getId());

            // If we want to cache, and have a local store that's newer -> update the local store
            // Note, if not caching then we won't update any local stores and won't cache the db store
            if (cacheStores && local.isPresent() && dbStore.getVersionField().get() >= local.get().getVersionField().get()) {
                StoreObjectCache.this.updateStoreFromNewer(local.get(), dbStore);
                StoreObjectCache.this.cache(dbStore);
            }

            // Find the store object to return
            @NotNull X ret = local.orElse(dbStore);
            // Verify it has the correct cache and cache it if necessary
            ret.setCache(StoreObjectCache.this);
            return ret;
        });
    }

    @NotNull
    @Override
    public Collection<X> getCached() {
        return localStore.getLocalCache().values();
    }

    @Override
    public boolean hasKeySync(@NotNull String key) {
        return localStore.has(key) || databaseStore.has(key);
    }

    @Override
    public @NotNull Optional<X> getFromCache(@NotNull String key) {
        return localStore.get(key);
    }

    @Override
    public @NotNull Optional<X> getFromDatabase(@NotNull String key, boolean cacheStore) {
        Optional<X> o = databaseStore.get(key);
        if (cacheStore) {
            o.ifPresent(this::cache);
        }
        return o;
    }

    @Override
    public void setLoggerService(@NotNull LoggerService loggerService) {
        this.loggerService = loggerService;
    }

    @Override
    public long getLocalCacheSize() {
        return localStore.size();
    }

    @Override
    public @NotNull Iterable<String> getIDs() {
        return databaseStore.getKeys();
    }

}
