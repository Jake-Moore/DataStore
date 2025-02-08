package com.kamikazejam.datastore.store.profile

import com.google.common.base.Preconditions
import com.kamikazejam.datastore.DataStoreSource
import com.kamikazejam.datastore.base.extensions.update
import com.kamikazejam.datastore.store.StoreProfile
import com.kamikazejam.datastore.store.profile.listener.ProfileListener
import com.kamikazejam.datastore.util.Color
import com.kamikazejam.datastore.util.DataStoreFileLogger
import com.kamikazejam.datastore.util.PlayerJoinDetails
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import java.util.UUID


@Suppress("unused")
open class StoreProfileLoader<X : StoreProfile<X>>(collection: StoreProfileCollection<X>) {
    protected val collection: StoreProfileCollection<X>

    /**
     * Whether this loader is being used during a login operation
     */
    var denyJoin: Boolean = false
    var joinDenyReason: String? = ChatColor.RED.toString() + "A caching error occurred. Please try again."
    protected var store: X? = null
    protected var player: Player? = null

    init {
        Preconditions.checkNotNull(collection)
        this.collection = collection
    }

    suspend fun fetchOnLogin(saveToLocalCache: Boolean, uuid: UUID, username: String, event: AsyncPlayerPreLoginEvent): X? {
        // Reset previous state
        denyJoin = false
        store = null

        // We are fetching (because of a login), check if we can write
        val storageService = DataStoreSource.storageService
        if (!storageService.canWriteToStorage()) {
            DataStoreSource.colorLogger.warn("StorageService is not ready to write objects, denying join")
            denyJoin = true
            joinDenyReason = Color.t(
                DataStoreSource.config.getString("profiles.messages.beforeDbConnection")
                    .replace("{collName}", collection.name)
            )
            return null
        }

        // Load details into this loader class
        try {
            this.store = loadOrCreateStore(collection, uuid, username, event)
        } catch (t: Throwable) {
            DataStoreFileLogger.warn("Failed to load or create StoreProfile from Database, denying join", t)
            this.denyJoin = true
            this.joinDenyReason = Color.t(
                DataStoreSource.config.getString("profiles.messages.beforeDbConnection")
                    .replace("{collName}", collection.name)
            )
        }

        // The above method will load the store into this variable if it exists
        val o: X? = this.store

        // Ensure the store is cached and has a valid collection reference
        o?.let { store ->
            if (saveToLocalCache) {
                collection.cache(store)
            } else {
                store.initialize(collection)
            }
        }
        return o
    }

    /**
     * Called in [ProfileListener.onProfileCachingInit]
     */
    fun initializeOnJoin(player: Player) {
        this.player = player
        if (store == null) {
            store = collection.readFromCache(player)
        }
        store?.initializePlayer(player)
    }



    // ------------------------------------------------------------ //
    //                         Helper Methods                       //
    // ------------------------------------------------------------ //

    private suspend fun <X : StoreProfile<X>> loadOrCreateStore(
        collection: StoreProfileCollection<X>,
        uuid: UUID,
        username: String,
        event: AsyncPlayerPreLoginEvent,
    ): X {
        // Try loading from local
        val localStore = collection.localStore.get(uuid)
        if (localStore != null) {
            return localStore
        }

        // Try loading from database
        val store: X? = collection.databaseStore.read(uuid)
        if (store == null) {
            // Make a new profile if they are logging in
            collection.getLoggerService().debug("Creating a new StoreProfile for: $username")
            return createStore(collection, uuid, username, event)
        }

        // We have a valid store from Database
        store.initialize(collection)

        // Update their username
        if (store.username != username) {
            // Attempt to save the new username
            collection.update(store) { x: X -> x.copyHelper(username) }
        }
        return store
    }

    private suspend fun <X : StoreProfile<X>> createStore(
        collection: ProfileCollection<X>,
        uuid: UUID,
        username: String,
        event: AsyncPlayerPreLoginEvent,
    ): X {
        try {
            // Create a new instance in modifiable state
            val initial: X = collection.instantiator(uuid, 0L, username)
            initial.initialize(collection)

            // Initialize the store (make sure the id and version were not changed)
            val store: X = collection.defaultInitializer(initial, PlayerJoinDetails(uuid, username, event))
            assert(store.id == uuid) { "StoreProfile ID must match uuid on Creation/Initialization!" }
            assert(store.version == 0L) { "StoreProfile version must be 0 on Creation/Initialization!" }

            // Save the store to our database implementation & cache
            // DO DATABASE SAVE FIRST SO ANY EXCEPTIONS ARE THROWN PRIOR TO MODIFYING LOCAL CACHE
            collection.databaseStore.save(store)
            collection.cache(store)
            return store
        } catch (e: Exception) {
            // promote error upwards for proper error handling
            throw e
        }
    }
}
