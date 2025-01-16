package com.kamikazejam.datastore.mode.profile.listener

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.kamikazejam.datastore.DataStoreAPI
import com.kamikazejam.datastore.DataStoreSource
import com.kamikazejam.datastore.base.exception.CachingError
import com.kamikazejam.datastore.event.profile.StoreProfileLoginEvent
import com.kamikazejam.datastore.event.profile.StoreProfileLogoutEvent
import com.kamikazejam.datastore.event.profile.StoreProfilePreLogoutEvent
import com.kamikazejam.datastore.mode.profile.ProfileCache
import com.kamikazejam.datastore.mode.profile.StoreProfile
import com.kamikazejam.datastore.mode.profile.StoreProfileCache
import com.kamikazejam.datastore.mode.profile.StoreProfileLoader
import com.kamikazejam.datastore.util.AsyncCachesExecutor
import com.kamikazejam.datastore.util.Color
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerLoginEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

/**
 * This class manages the synchronization between a bukkit [Player] and their [StoreProfile] objects.<br></br>
 * Its primary purpose it to listen to joins/quits and ensure caches are loaded & unloaded as players join and leave.
 */
@Suppress("unused", "UnstableApiUsage")
class ProfileListener : Listener {
    private val loginCache: Cache<UUID, Long?> =
        CacheBuilder.newBuilder().expireAfterWrite(100, TimeUnit.MILLISECONDS).build()

    @EventHandler(priority = EventPriority.LOW)
    fun onProfileCachingStart(event: AsyncPlayerPreLoginEvent) {
        val ms = System.currentTimeMillis()
        val username: String = event.name
        val uniqueId: UUID = event.uniqueId
        val ip: String = event.address.hostAddress

        val storageService = DataStoreSource.storageService
        if (!storageService.canCache() || DataStoreSource.onEnableTime <= 0) {
            DataStoreSource.colorLogger.warn("StorageService is not ready to cache objects, denying join")
            event.disallow(
                AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                ChatColor.RED.toString() + "Server is starting, please wait."
            )
            return
        }

        // Run a special fully parallelized execution for caches based on their depends
        try {
            val timeout: Long = 5 // seconds
            cachePlayerProfiles(username, uniqueId, ip, timeout)[timeout + 3, TimeUnit.SECONDS]
        } catch (t: Throwable) {
            if (t is ExecutionException) {
                if (t.cause is CachingError) {
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, (t.cause as CachingError).message)
                    return
                }
            }

            t.printStackTrace()
            event.disallow(
                AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                ChatColor.RED.toString() + "A caching error occurred.  Please try again."
            )
            return
        }

        // Save async to prevent unnecessary blocking on join
        storageService.debug("Player " + username + " (" + uniqueId + ") profiles loaded in " + (System.currentTimeMillis() - ms) + "ms")
        loginCache.put(uniqueId, System.currentTimeMillis())
    }

    @Suppress("UNCHECKED_CAST", "UNUSED_PARAMETER", "SameParameterValue")
    private fun <X : StoreProfile<X>> cachePlayerProfiles(
        username: String,
        uniqueId: UUID,
        ip: String,
        timeoutSec: Long
    ): CompletableFuture<Void> {
        // Compile all the ProfileCaches
        val caches: MutableList<StoreProfileCache<X>> = ArrayList()
        DataStoreAPI.caches.values.forEach { c: com.kamikazejam.datastore.base.Cache<*, *>? ->
            if (c is StoreProfileCache<*>) {
                caches.add(c as StoreProfileCache<X>)
            }
        }
        val executor = AsyncCachesExecutor(caches, { cache ->
                val loader: StoreProfileLoader<X> = cache.loader(uniqueId)
                loader.login(username)
                loader.fetch(true)
                if (loader.denyJoin) {
                    // For the first 100 seconds, don't give the nasty loader reason, but a pretty server start error
                    val message = if (System.currentTimeMillis() - DataStoreSource.onEnableTime < 100000L)
                        Color.t("&cServer is starting, please wait.")
                    else
                        loader.joinDenyReason

                    // If denied, throw an exception (will be caught by original join event)
                    throw CachingError(message)
                }
            }, timeoutSec
        )

        // Execute the cache list in order
        return executor.executeInOrder()
    }

    @EventHandler(priority = EventPriority.LOW)
    fun onProfileCachingInit(event: PlayerLoginEvent) {
        // In rare cases, if a player joins during server startup, the AsyncPlayerPreLoginEvent may not have been called
        if (loginCache.getIfPresent(event.player.uniqueId) == null) {
            DataStoreSource.colorLogger.warn("Player (" + event.player.name + ") connected before DataStore was ready, denying join")
            event.disallow(
                PlayerLoginEvent.Result.KICK_OTHER,
                ChatColor.RED.toString() + "Server is starting, please wait."
            )
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    fun onProfileCachingInit(event: PlayerJoinEvent) {
        val player: Player = event.player
        DataStoreAPI.caches.values.forEach { c: com.kamikazejam.datastore.base.Cache<*, *> ->
            if (c is StoreProfileCache<*>) {
                val loader: StoreProfileLoader<*> = c.loader(player.uniqueId)
                loader.initializeOnJoin(player)
            }
        }

        // Call the event now that caches are loaded
        val e = StoreProfileLoginEvent(player)
        Bukkit.getServer().pluginManager.callEvent(e)
    }

    @EventHandler(priority = EventPriority.MONITOR) // Run LAST
    fun onProfileQuit(event: PlayerQuitEvent) {
        val player: Player = event.player

        // Call the event now that caches are unloaded
        val preLogoutEvent = StoreProfilePreLogoutEvent(player)
        Bukkit.getServer().pluginManager.callEvent(preLogoutEvent)

        // Best way to do this for now, is to do this sync and in order of reverse depends
        DataStoreAPI.sortedCachesByDependsReversed.forEach { c: com.kamikazejam.datastore.base.Cache<*, *>? ->
            if (c is StoreProfileCache<*>) {
                quit(player, c)
            }
        }

        // TODO ensure the StoreProfile is removed from cache and not being a memory leak

        // Call the event now that caches are unloaded
        val logoutEvent = StoreProfileLogoutEvent(player)
        Bukkit.getServer().pluginManager.callEvent(logoutEvent)
    }

    companion object {
        fun <X : StoreProfile<X>> quit(player: Player, cache: ProfileCache<X>) {
            cache.getLoggerService().debug("Player " + player.name + " quitting, saving profile...")
            quitHelper(player, cache)
        }

        private fun <X : StoreProfile<X>> quitHelper(player: Player, cache: ProfileCache<X>) {
            // save on quit in standalone mode
            val o: X? = cache.getFromCache(player.uniqueId)
            o?.let { profile ->
                // ModificationRequest can be ignored since we are saving below
                cache.onProfileLeaving(player, profile)
                profile.uninitializePlayer()

                // clean up the cache
                cache.removeLoader(profile.uniqueId)
            }
        }
    }
}