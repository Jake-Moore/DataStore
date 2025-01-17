package com.kamikazejam.datastore.mode.profile

import com.kamikazejam.datastore.base.Collection
import com.kamikazejam.datastore.base.result.AsyncStoreHandler
import org.bukkit.entity.Player
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.Blocking
import org.jetbrains.annotations.NonBlocking
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer

/**
 * Defines Profile-specific getters for StoreObjects. They return non-null Optionals.
 * All get options (when not handshaking) will create if necessary. This is because every
 * Player UUID is assumed to have a StoreProfile
 */
@Suppress("unused", "BlockingMethodInNonBlockingContext")
interface ProfileCollection<X : StoreProfile<X>> :
    Collection<UUID, X> {
    // ----------------------------------------------------- //
    //                 CRUD Helpers (Async)                  //
    // ----------------------------------------------------- //
    /**
     * Read a StoreProfile (by player) from this cache (will fetch from database, will create if necessary)
     * @param player The player owning this store.
     * @return The StoreProfile object. (READ-ONLY)
     */
    @NonBlocking
    fun read(player: Player): AsyncStoreHandler<X> {
        return this.read(player, true)
    }

    /**
     * Read a StoreProfile (by player) from this cache (will fetch from database, will create if necessary)
     * @param player The player owning this store.
     * @param cacheStore If we should cache the Store upon retrieval. (if it was found)
     * @return The StoreProfile object. (READ-ONLY)
     */
    @NonBlocking
    fun read(player: Player, cacheStore: Boolean): AsyncStoreHandler<X> {
        return AsyncStoreHandler.of(CompletableFuture.supplyAsync { this.readSync(player, cacheStore) }, this)
    }

    /**
     * Modifies a Store in a controlled environment where modifications are allowed
     * @throws NoSuchElementException if the Store (by this key) is not found
     * @return The updated Store object. (READ-ONLY)
     */
    @NonBlocking
    fun update(player: Player, updateFunction: Consumer<X>): AsyncStoreHandler<X> {
        return AsyncStoreHandler.of(CompletableFuture.supplyAsync { this.updateSync(player, updateFunction) }, this)
    }

    /**
     * Deletes a Store (removes from both cache and database)
     */
    @NonBlocking
    fun delete(player: Player): AsyncStoreHandler<Void> {
        return AsyncStoreHandler.of<Void>(CompletableFuture.runAsync { this.deleteSync(player) }, this)
    }


    // ----------------------------------------------------- //
    //                  CRUD Helpers (sync)                  //
    // ----------------------------------------------------- //
    /**
     * Read a StoreProfile (by player) from this cache (will fetch from database, will create if necessary)
     * @param player The player owning this store.
     * @return The StoreProfile object. (READ-ONLY)
     */
    @Blocking
    fun readSync(player: Player): X {
        return this.readSync(player, true)
    }

    /**
     * Read a StoreProfile (by player) from this cache (will fetch from database, will create if necessary)
     * @param player The player owning this store.
     * @param cacheStore If we should cache the Store upon retrieval. (if it was found)
     * @return The StoreProfile object. (READ-ONLY)
     */
    @Blocking
    fun readSync(player: Player, cacheStore: Boolean): X

    /**
     * Modifies a Store in a controlled environment where modifications are allowed
     * @throws NoSuchElementException if the Store (by this key) is not found
     * @return The updated Store object. (READ-ONLY)
     */
    @Blocking
    fun updateSync(player: Player, updateFunction: Consumer<X>): X {
        return this.updateSync(player.uniqueId, updateFunction)
    }

    /**
     * Deletes a Store (removes from both cache and database)
     */
    @Blocking
    fun deleteSync(player: Player) {
        this.deleteSync(player.uniqueId)
    }

    // ------------------------------------------------------ //
    // Cache Methods                                          //
    // ------------------------------------------------------ //
    /**
     * Retrieve a Store from this cache (by player).
     * This method does NOT query the database.
     * @return The Store if it was cached.
     */
    @NonBlocking
    fun getFromCache(player: Player): X?

    /**
     * Retrieve a Store from the database (by player).
     * This method force queries the database, and updates this cache.
     * @param cacheStore If we should cache the Store upon retrieval. (if it was found)
     * @return The Store if it was found in the database.
     */
    @Blocking
    fun getFromDatabase(player: Player, cacheStore: Boolean): X?

    /**
     * Gets all online players' Profile objects. These should all be in the cache.
     */
    val online: kotlin.collections.Collection<X>

    @ApiStatus.Internal
    fun removeLoader(uuid: UUID)

    fun onProfileLeaving(player: Player, profile: X)
}
