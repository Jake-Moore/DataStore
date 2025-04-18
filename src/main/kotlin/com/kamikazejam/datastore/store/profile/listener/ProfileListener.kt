package com.kamikazejam.datastore.store.profile.listener

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.kamikazejam.datastore.DataStoreAPI
import com.kamikazejam.datastore.DataStoreSource
import com.kamikazejam.datastore.api.event.StoreProfileLoginEvent
import com.kamikazejam.datastore.api.event.StoreProfileLogoutEvent
import com.kamikazejam.datastore.api.event.StoreProfilePreLogoutEvent
import com.kamikazejam.datastore.base.Collection
import com.kamikazejam.datastore.base.exception.ProfileDenyJoinError
import com.kamikazejam.datastore.store.StoreProfile
import com.kamikazejam.datastore.store.profile.ProfileCollection
import com.kamikazejam.datastore.store.profile.StoreProfileCollection
import com.kamikazejam.datastore.store.profile.StoreProfileLoader
import com.kamikazejam.datastore.util.AsyncCollectionsExecutor
import com.kamikazejam.datastore.util.Color
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.time.withTimeout
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
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

/**
 * This class manages the synchronization between a bukkit [Player] and their [StoreProfile] objects.<br></br>
 * Its primary purpose it to listen to joins/quits and ensure collections are loaded & unloaded as players join and leave.
 */
@Suppress("unused", "UnstableApiUsage", "UNUSED_VARIABLE")
class ProfileListener : Listener {
    private val loginCache: Cache<UUID, Long> =
        CacheBuilder.newBuilder().expireAfterWrite(15, TimeUnit.SECONDS).build()

    @EventHandler(priority = EventPriority.LOW)
    fun onProfileCachingStart(event: AsyncPlayerPreLoginEvent) {
        val ms = System.currentTimeMillis()
        val username: String = event.name
        val uniqueId: UUID = event.uniqueId

        val storageService = DataStoreSource.storageService
        if (!storageService.canWriteToStorage() || DataStoreSource.onEnableTime <= 0) {
            DataStoreSource.colorLogger.warn("StorageService is not ready to write objects, denying join")
            event.disallow(
                AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                ChatColor.RED.toString() + "Server is starting, please wait."
            )
            return
        }

        // Block while we preload all the player's profiles
        val timeout: Long = 5 // seconds
        val ignored = runBlocking {
            try {
                withTimeout(Duration.of(timeout + 3, ChronoUnit.SECONDS)) {
                    // Run a special fully parallelized execution for collections based on their depends
                    cachePlayerProfiles(username, uniqueId, event, timeout)
                }
            } catch (t: Throwable) {
                var message: String = ChatColor.RED.toString() + "A caching error occurred. Please try again."
                val cause = t.cause
                if (t is ExecutionException && cause is ProfileDenyJoinError) {
                    message = cause.msg
                }

                t.printStackTrace()
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, message)
            }
        }

        // Save async to prevent unnecessary blocking on join
        storageService.debug("Player " + username + " (" + uniqueId + ") profiles loaded in " + (System.currentTimeMillis() - ms) + "ms")
        loginCache.put(uniqueId, System.currentTimeMillis())
    }

    @Suppress("UNCHECKED_CAST", "SameParameterValue")
    private fun <X : StoreProfile<X>> cachePlayerProfiles(
        username: String,
        uniqueId: UUID,
        event: AsyncPlayerPreLoginEvent,
        timeoutSec: Long
    ): Deferred<Unit> {
        // Compile all the ProfileCaches
        val collections: MutableList<StoreProfileCollection<X>> = ArrayList()
        DataStoreAPI.collections.values.forEach { c: Collection<*, *>? ->
            if (c is StoreProfileCollection<*>) {
                collections.add(c as StoreProfileCollection<X>)
            }
        }
        val executor = AsyncCollectionsExecutor(collections, timeoutSec) {
            val loader: StoreProfileLoader<X> = it.loader(uniqueId)
            loader.fetchOnLogin(true, uniqueId, username, event)
            val reason = loader.joinDenyReason
            if (reason != null) {
                // If denied, throw an exception (will be caught by original join event)
                throw ProfileDenyJoinError(Color.tNotNull(reason))
            }
        }

        // Execute the collection list in order
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
        loginCache.invalidate(event.player.uniqueId)
    }

    @EventHandler(priority = EventPriority.LOW)
    fun onProfileCachingInit(event: PlayerJoinEvent) {
        val player: Player = event.player
        DataStoreAPI.collections.values.forEach { c: Collection<*, *> ->
            if (c is StoreProfileCollection<*>) {
                val loader: StoreProfileLoader<*> = c.loader(player.uniqueId)
                loader.initializeOnJoin(player)
            }
        }

        // Call the event now that collections are loaded
        val e = StoreProfileLoginEvent(player)
        Bukkit.getServer().pluginManager.callEvent(e)
    }

    @EventHandler(priority = EventPriority.MONITOR) // Run LAST
    fun onProfileQuit(event: PlayerQuitEvent) {
        val player: Player = event.player

        // Call the event now that collections are unloaded
        val preLogoutEvent = StoreProfilePreLogoutEvent(player)
        Bukkit.getServer().pluginManager.callEvent(preLogoutEvent)

        // Best way to do this for now, is to do this sync and in order of reverse depends
        DataStoreAPI.sortedCollectionsByDependsReversed.forEach { c: Collection<*, *>? ->
            if (c is StoreProfileCollection<*>) {
                quit(player, c)
            }
        }

        // TODO ensure the StoreProfile is removed from collection and not being a memory leak

        // Call the event now that collections are unloaded
        val logoutEvent = StoreProfileLogoutEvent(player)
        Bukkit.getServer().pluginManager.callEvent(logoutEvent)
    }

    companion object {
        fun <X : StoreProfile<X>> quit(player: Player, collection: ProfileCollection<X>) {
            collection.getLoggerService().debug("Player " + player.name + " quitting, saving profile...")
            quitHelper(player, collection)
        }

        private fun <X : StoreProfile<X>> quitHelper(player: Player, collection: ProfileCollection<X>) {
            // save on quit in standalone mode
            val o: X? = collection.readFromCache(player.uniqueId)
            o?.let { profile ->
                // ModificationRequest can be ignored since we are saving below
                collection.onProfileLeaving(player, profile)
                profile.uninitializePlayer()

                // clean up the collection
                collection.removeLoader(profile.uniqueId)
            }
        }
    }
}
