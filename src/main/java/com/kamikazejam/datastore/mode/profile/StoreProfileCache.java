package com.kamikazejam.datastore.mode.profile;

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
import com.kamikazejam.datastore.event.profile.StoreProfileCacheQuitEvent;
import com.kamikazejam.datastore.mode.profile.store.ProfileStorageDatabase;
import com.kamikazejam.datastore.mode.profile.store.ProfileStorageLocal;
import com.kamikazejam.kamicommon.util.PlayerUtil;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Getter
@SuppressWarnings("unused")
public abstract class StoreProfileCache<X extends StoreProfile<X>> extends StoreCache<UUID, X> implements ProfileCache<X> {

    private final ConcurrentMap<UUID, StoreProfileLoader<X>> loaders = new ConcurrentHashMap<>();
    private final ProfileStorageLocal<X> localStore = new ProfileStorageLocal<>();
    private final ProfileStorageDatabase<X> databaseStore = new ProfileStorageDatabase<>(this);
    
    public StoreProfileCache(DataStoreRegistration module, StoreInstantiator<UUID, X> instantiator, String name, Class<X> storeClass) {
        // Optional Constructor that will use the default CacheLoggerService
        this(module, instantiator, name, storeClass, CacheLoggerService::new);
    }

    public StoreProfileCache(DataStoreRegistration module, StoreInstantiator<UUID, X> instantiator, String name, Class<X> storeClass, CacheLoggerInstantiator logger) {
        super(instantiator, name, UUID.class, storeClass, module, logger);

        // Start this cache
        if (!start()) {
            // Data loss is not tolerated in DataStore, shutdown to prevent issues
            DataStoreSource.get().getLogger().severe("Failed to start Profile Cache: " + name);
            Bukkit.shutdown();
        }
    }

    @Override
    protected boolean initialize() {
        // nothing to do here
        return true;
    }

    @Override
    protected boolean terminate() {
        loaders.clear();
        // Clear locals store (frees memory)
        localStore.clear();
        // Don't clear database (can't)

        return true;
    }

    // ----------------------------------------------------- //
    //                          CRUD                         //
    // ----------------------------------------------------- //
    @Override
    public @NotNull X read(@NotNull Player player, boolean cacheStore) {
        X store = StoreProfileLoader.loadOrCreateStore(this, player.getUniqueId(), true, player.getName());
        if (cacheStore) { this.cache(store); }
        return store;
    }

    @Override
    public @NotNull Iterable<X> readAll(boolean cacheStores) {
        return () -> new TransformingIterator<>(this.getIDs().iterator(), id -> read(id, cacheStores).orElse(null));
    }

    @Override
    public @NotNull Iterable<X> readAllFromDatabase(boolean cacheStores) {
        return this.getDatabaseStore().getAll();
    }

    // ----------------------------------------------------- //
    //                         Cache                         //
    // ----------------------------------------------------- //
    @NotNull
    @Override
    public StoreProfileLoader<X> loader(@NotNull UUID key) {
        Preconditions.checkNotNull(key);
        return loaders.computeIfAbsent(key, s -> new StoreProfileLoader<>(this, s));
    }

    @Override
    public @NotNull StorageDatabase<UUID, X> getDatabaseStore() {
        return databaseStore;
    }

    @Override
    public @NotNull String keyToString(@NotNull UUID key) {
        return key.toString();
    }

    @Override
    public @NotNull UUID keyFromString(@NotNull String key) {
        Preconditions.checkNotNull(key);
        return UUID.fromString(key);
    }

    @NotNull
    @Override
    public Collection<X> getCached() {
        return localStore.getLocalCache().values();
    }

    @Override
    public boolean hasKey(@NotNull UUID key) {
        return localStore.has(key) || databaseStore.has(key);
    }

    @Override
    public @NotNull Optional<X> getFromCache(@NotNull UUID key) {
        return this.localStore.get(key);
    }

    @Override
    public @NotNull Optional<X> getFromDatabase(@NotNull UUID key, boolean cacheStore) {
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
    public @NotNull Iterable<UUID> getIDs() {
        return databaseStore.getKeys();
    }



    // ----------------------------------------------------- //
    //                     ProfileCache                      //
    // ----------------------------------------------------- //

    @NotNull
    @Override
    public Set<X> getOnline() {
        // Stream online players and map them to their StoreProfile
        return Bukkit.getOnlinePlayers().stream()
                .filter(PlayerUtil::isFullyValidPlayer)
                .map(this::read)
                .collect(Collectors.toSet());
    }

    @Override
    public Optional<X> getFromCache(@NotNull Player player) {
        Preconditions.checkNotNull(player);
        return getFromCache(player.getUniqueId());
    }

    @Override
    public Optional<X> getFromDatabase(@NotNull Player player, boolean cacheStore) {
        Preconditions.checkNotNull(player);
        Optional<X> o = databaseStore.get(player.getUniqueId());
        if (cacheStore) {
            o.ifPresent(this::cache);
        }
        return o;
    }

    @Override
    public void removeLoader(@NotNull UUID uuid) {
        Preconditions.checkNotNull(uuid);
        this.loaders.remove(uuid);
    }

    @Override
    public final void onProfileLeaving(@NotNull Player player, @NotNull X profile) {
        StoreProfileCacheQuitEvent<X> event = new StoreProfileCacheQuitEvent<>(player, this, profile);
        Bukkit.getPluginManager().callEvent(event);
    }

}
