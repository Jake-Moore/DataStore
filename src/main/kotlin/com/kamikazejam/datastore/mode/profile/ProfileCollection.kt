package com.kamikazejam.datastore.mode.profile

import com.kamikazejam.datastore.base.Collection
import org.bukkit.entity.Player
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.Blocking
import org.jetbrains.annotations.NonBlocking
import java.util.*

/**
 * Defines Profile-specific getters for StoreObjects. They return non-null Optionals.
 * All get options (when not handshaking) will create if necessary. This is because every
 * Player UUID is assumed to have a StoreProfile
 */
@Suppress("unused")
interface ProfileCollection<X : StoreProfile<X>> : Collection<UUID, X> {

    // ------------------------------------------------------ //
    // Cache Methods                                          //
    // ------------------------------------------------------ //
    /**
     * Retrieve a Store from this collection (by player).
     * This method does NOT query the database.
     * @return The Store if it was cached.
     */
    @NonBlocking
    fun readFromCache(player: Player): X?

    /**
     * Retrieve a Store from the database (by player).
     * This method force queries the database, and updates this cache.
     * @param cacheStore If we should cache the Store upon retrieval. (if it was found)
     * @return The Store if it was found in the database.
     */
    @Blocking
    suspend fun readFromDatabase(player: Player, cacheStore: Boolean): X?

    /**
     * Gets all online players' Profile objects. These should all be present the collection and cached.
     */
    suspend fun getOnline(): kotlin.collections.Collection<X>

    @ApiStatus.Internal
    fun removeLoader(uuid: UUID)

    fun onProfileLeaving(player: Player, profile: X)
}
