package com.kamikazejam.datastore.mode.profile.listener;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.kamikazejam.datastore.DataStoreAPI;
import com.kamikazejam.datastore.DataStoreSource;
import com.kamikazejam.datastore.base.exception.CachingError;
import com.kamikazejam.datastore.connections.storage.StorageService;
import com.kamikazejam.datastore.event.profile.StoreProfileLoginEvent;
import com.kamikazejam.datastore.event.profile.StoreProfileLogoutEvent;
import com.kamikazejam.datastore.event.profile.StoreProfilePreLogoutEvent;
import com.kamikazejam.datastore.mode.profile.ProfileCache;
import com.kamikazejam.datastore.mode.profile.StoreProfile;
import com.kamikazejam.datastore.mode.profile.StoreProfileCache;
import com.kamikazejam.datastore.mode.profile.StoreProfileLoader;
import com.kamikazejam.datastore.util.AsyncCachesExecutor;
import com.kamikazejam.datastore.util.Color;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * This class manages the synchronization between a bukkit {@link Player} and their {@link StoreProfile} objects.<br>
 * Its primary purpose it to listen to joins/quits and ensure caches are loaded & unloaded as players join and leave.
 */
@SuppressWarnings({"unused", "UnstableApiUsage"})
public class ProfileListener implements Listener {

    public ProfileListener() {}

    public static <X extends StoreProfile<X>> void quit(@NotNull Player player, ProfileCache<X> cache, boolean isEnabled) {
        cache.getLoggerService().debug("Player " + player.getName() + " quitting, saving profile...");
        try {
            quitHelper(player, cache, isEnabled);
        }catch (IllegalPluginAccessException e) {
            // try again synchronously
            quitHelper(player, cache, false);
        }
    }

    private static <X extends StoreProfile<X>> void quitHelper(@NotNull Player player, ProfileCache<X> cache, boolean saveAsync) {
        // save on quit in standalone mode
        Optional<X> o = cache.getFromCache(player.getUniqueId());
        if (o.isPresent()) {
            X profile = o.get();

            // ModificationRequest can be ignored since we are saving below
            cache.onProfileLeaving(player, profile);
            profile.uninitializePlayer();

            // clean up the cache
            cache.removeLoader(profile.getUniqueId());
        }
    }

    private final Cache<UUID, Long> loginCache = CacheBuilder.newBuilder().expireAfterWrite(100, TimeUnit.MILLISECONDS).build();
    @EventHandler(priority = EventPriority.LOW)
    public void onProfileCachingStart(AsyncPlayerPreLoginEvent event) {
        final long ms = System.currentTimeMillis();
        final String username = event.getName();
        final UUID uniqueId = event.getUniqueId();
        final String ip = event.getAddress().getHostAddress();

        StorageService storageService = DataStoreSource.getStorageService();
        if (!storageService.canCache() || DataStoreSource.getOnEnableTime() <= 0) {
            DataStoreSource.get().getColorLogger().warn("StorageService is not ready to cache objects, denying join");
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, ChatColor.RED + "Server is starting, please wait.");
            return;
        }

        // Run a special fully parallelized execution for caches based on their depends
        try {
            long timeout = 5; // seconds
            cachePlayerProfiles(username, uniqueId, ip, timeout).get(timeout + 3, TimeUnit.SECONDS);
        }catch (Throwable t) {
            if (t instanceof ExecutionException e) {
                if (e.getCause() instanceof CachingError error) {
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, error.getMessage());
                    return;
                }
            }

            t.printStackTrace();
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, ChatColor.RED + "A caching error occurred.  Please try again.");
            return;
        }

        // Save async to prevent unnecessary blocking on join
        storageService.debug("Player " + username + " (" + uniqueId + ") profiles loaded in " + (System.currentTimeMillis() - ms) + "ms");
        loginCache.put(uniqueId, System.currentTimeMillis());
    }


    @SuppressWarnings("unchecked")
    private <X extends StoreProfile<X>> CompletableFuture<Void> cachePlayerProfiles(String username, UUID uniqueId, String ip, long timeoutSec) {
        // Compile all the ProfileCaches
        List<StoreProfileCache<X>> caches = new ArrayList<>();
        DataStoreAPI.getCaches().values().forEach(c -> {
            if (c instanceof StoreProfileCache<?>) { caches.add((StoreProfileCache<X>) c); }
        });
        AsyncCachesExecutor<StoreProfileCache<X>> executor = new AsyncCachesExecutor<>(caches, (cache) -> {
            StoreProfileLoader<X> loader = cache.loader(uniqueId);
            loader.login(username);
            loader.fetch(true);

            if (loader.isDenyJoin()) {
                // For the first 100 seconds, don't give the nasty loader reason, but a pretty server start error
                String message = (System.currentTimeMillis() - DataStoreSource.getOnEnableTime() < 100_000L)
                        ? Color.t("&cServer is starting, please wait.")
                        : loader.getJoinDenyReason();

                // If denied, throw an exception (will be caught by original join event)
                throw new CachingError(message);
            }
        }, timeoutSec);

        // Execute the cache list in order
        return executor.executeInOrder();
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onProfileCachingInit(PlayerLoginEvent event) {
        // In rare cases, if a player joins during server startup, the AsyncPlayerPreLoginEvent may not have been called
        if (loginCache.getIfPresent(event.getPlayer().getUniqueId()) == null) {
            DataStoreSource.get().getColorLogger().warn("Player (" + event.getPlayer().getName() + ") connected before DataStore was ready, denying join");
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, ChatColor.RED + "Server is starting, please wait.");
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onProfileCachingInit(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        DataStoreAPI.getCaches().values().forEach(c -> {
            if (c instanceof StoreProfileCache<?> cache) {
                StoreProfileLoader<?> loader = cache.loader(player.getUniqueId());
                loader.initializeOnJoin(player);
            }
        });

        // Call the event now that caches are loaded
        StoreProfileLoginEvent e = new StoreProfileLoginEvent(player);
        Bukkit.getServer().getPluginManager().callEvent(e);
    }

    @EventHandler(priority = EventPriority.MONITOR) // Run LAST
    public void onProfileQuit(final PlayerQuitEvent event) {
        final Player player = event.getPlayer();

        // Call the event now that caches are unloaded
        StoreProfilePreLogoutEvent preLogoutEvent = new StoreProfilePreLogoutEvent(player);
        Bukkit.getServer().getPluginManager().callEvent(preLogoutEvent);

        // Best way to do this for now, is to do this sync and in order of reverse depends
        DataStoreAPI.getSortedCachesByDependsReversed().forEach(c -> {
            if (c instanceof StoreProfileCache<?> cache) {
                quit(player, cache, true);
            }
        });

        // TODO ensure the StoreProfile is removed from cache and not being a memory leak

        // Call the event now that caches are unloaded
        StoreProfileLogoutEvent logoutEvent = new StoreProfileLogoutEvent(player);
        Bukkit.getServer().getPluginManager().callEvent(logoutEvent);
    }
}
