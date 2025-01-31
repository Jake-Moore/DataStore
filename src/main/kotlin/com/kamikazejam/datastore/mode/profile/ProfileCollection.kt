package com.kamikazejam.datastore.mode.profile

import com.kamikazejam.datastore.base.Collection
import com.kamikazejam.datastore.mode.store.StoreProfile
import org.bukkit.entity.Player
import org.jetbrains.annotations.ApiStatus.Internal
import org.jetbrains.annotations.Blocking
import org.jetbrains.annotations.NonBlocking
import java.util.UUID

/**
 * Defines Profile-specific getters for StoreObjects. They return non-null Optionals.
 * All get options (when not handshaking) will create if necessary. This is because every
 * Player UUID is assumed to have a StoreProfile
 */
@Suppress("unused")
interface ProfileCollection<X : StoreProfile<X>> : Collection<UUID, X> {

    /**
     * How we create a new instance of the [StoreProfile] for this [Collection].
     *
     * Given the ID [UUID], the version [Long], and the username [String], return a new instance
     */
    val instantiator: (UUID, Long, String?) -> X

    /**
     * [StoreProfile] objects are often created by DataStore (for example, when a player joins the server).
     *
     * This property allows you to define a default initializer for new [StoreProfile] objects, to initialize them with default values.
     */
    val defaultInitializer: (X) -> X

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

    @Internal
    fun removeLoader(uuid: UUID)

    @Internal
    fun onProfileLeaving(player: Player, profile: X)
}
