package com.kamikazejam.datastore.mode.profile;

import com.google.common.base.Preconditions;
import com.kamikazejam.datastore.DataStoreSource;
import com.kamikazejam.datastore.base.cache.StoreLoader;
import com.kamikazejam.datastore.connections.storage.StorageService;
import com.kamikazejam.datastore.mode.profile.listener.ProfileListener;
import com.kamikazejam.datastore.util.DataStoreFileLogger;
import com.kamikazejam.kamicommon.util.StringUtil;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

@Getter
@Setter @SuppressWarnings("unused")
public class StoreProfileLoader<X extends StoreProfile<X>> implements StoreLoader<X> {

    protected final @NotNull StoreProfileCache<X> cache;
    protected final @NotNull UUID uuid;
    protected String username = null;
    /**
     * Whether this loader is being used during a login operation
     */
    protected boolean login = false;
    protected boolean denyJoin = false;
    protected String joinDenyReason = ChatColor.RED + "A caching error occurred. Please try again.";
    protected @Nullable X store = null;
    protected Player player = null;

    public StoreProfileLoader(@NotNull StoreProfileCache<X> cache, @NotNull UUID uuid) {
        Preconditions.checkNotNull(cache);
        Preconditions.checkNotNull(uuid);
        this.cache = cache;
        this.uuid = uuid;
    }

    @Override
    public Optional<X> fetch(boolean saveToLocalCache) {
        // Reset previous state
        denyJoin = false;
        store = null;

        // If we are fetching (because of a login), check if we can cache
        if (login) {
            StorageService storageService = DataStoreSource.getStorageService();
            if (!storageService.canCache()) {
                DataStoreSource.get().getColorLogger().warn("StorageService is not ready to cache objects, denying join");
                denyJoin = true;
                joinDenyReason = StringUtil.t(DataStoreSource.getConfig().getString("profiles.messages.beforeDbConnection")
                        .replace("{cacheName}", cache.getName()));
                return Optional.empty();
            }
        }

        // Load details into this loader class
        try {
            this.store = loadOrCreateStore(cache, uuid, login, username);
        }catch (Throwable t) {
            DataStoreFileLogger.warn("Failed to load or create StoreProfile from Database, denying join", t);
            this.denyJoin = true;
            this.joinDenyReason = StringUtil.t(DataStoreSource.getConfig().getString("profiles.messages.beforeDbConnection")
                    .replace("{cacheName}", cache.getName()));
        }

        // The above method will load the store into this variable if it exists
        Optional<X> o = Optional.ofNullable(this.store);

        // Ensure the store is cached and has a valid cache reference
        o.ifPresent(store -> {
            @Nullable Player p = Bukkit.getPlayer(store.getUniqueId());
            if (saveToLocalCache) {
                cache.cache(store);
            }else {
                store.setCache(cache);
            }
        });
        return o;
    }

    public void login(@NotNull String username) {
        this.login = true;
        this.username = username;
    }

    /**
     * Called in {@link ProfileListener#onProfileCachingInit(PlayerJoinEvent)}
     */
    public void initializeOnJoin(Player player) {
        this.player = player;
        if (store == null) {
            store = cache.getFromCache(player).orElse(null);
        }
        if (store != null) {
            store.initializePlayer(player);
        }
    }

    @NotNull
    public static <X extends StoreProfile<X>> X loadOrCreateStore(StoreProfileCache<X> cache, UUID uuid, boolean creative, String username) {
        // Try loading from local
        Optional<X> localStore = cache.getLocalStore().get(uuid);
        if (localStore.isPresent()) {
            return localStore.get();
        }

        // Try loading from database
        Optional<X> o = cache.getDatabaseStore().get(uuid);
        if (o.isEmpty()) {
            // Make a new profile if they are logging in
            if (creative) {
                cache.getLoggerService().debug("Creating a new StoreProfile for: " + username);
                return createStore(cache, uuid, username, store -> store.setCache(cache));
            }

            // Assume some other kind of failure:
            throw new RuntimeException("Failed to load or create StoreProfile from Database");
        }

        // We have a valid store from Database
        final X store = o.get();
        store.setCache(cache);

        // For logins -> mark as loaded
        if (creative) {
            // Update their username
            if (username != null && !store.username.get().equals(username)) {
                // Attempt to save the new username
                cache.update(store, x -> x.username.set(username));
            }
        }
        return store;
    }

    @NotNull
    private static <X extends StoreProfile<X>> X createStore(ProfileCache<X> cache, @NotNull UUID uuid, @Nullable String username, Consumer<X> initializer) {
        try {
            // Create a new instance in modifiable state
            X store = cache.getInstantiator().instantiate();
            store.initialize();
            store.setReadOnly(false);

            // Initialize the store
            initializer.accept(store);
            // Enforce Version 0 for creation
            store.id.set(uuid);
            store.version.set(0L);
            store.username.set(username);

            store.setReadOnly(true);

            // Save the store to our database implementation & cache
            cache.cache(store);
            cache.getDatabaseStore().save(store);
            return store;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Store", e);
        }
    }
}
