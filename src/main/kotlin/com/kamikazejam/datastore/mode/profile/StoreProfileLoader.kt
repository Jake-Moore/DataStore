package com.kamikazejam.datastore.mode.profile

import com.google.common.base.Preconditions
import com.kamikazejam.datastore.DataStoreSource
import com.kamikazejam.datastore.base.cache.StoreLoader
import com.kamikazejam.datastore.mode.profile.listener.ProfileListener
import com.kamikazejam.datastore.util.Color
import com.kamikazejam.datastore.util.DataStoreFileLogger
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import java.util.*
import java.util.function.Consumer


@Suppress("unused")
open class StoreProfileLoader<X : StoreProfile<X>>(cache: StoreProfileCache<X>, uuid: UUID) : StoreLoader<X> {
    protected val cache: StoreProfileCache<X>
    protected val uuid: UUID
    private var username: String? = null

    /**
     * Whether this loader is being used during a login operation
     */
    private var login: Boolean = false
    var denyJoin: Boolean = false
    var joinDenyReason: String? = ChatColor.RED.toString() + "A caching error occurred. Please try again."
    protected var store: X? = null
    protected var player: Player? = null

    init {
        Preconditions.checkNotNull(cache)
        Preconditions.checkNotNull(uuid)
        this.cache = cache
        this.uuid = uuid
    }

    override fun fetch(saveToLocalCache: Boolean): X? {
        // Reset previous state
        denyJoin = false
        store = null

        // If we are fetching (because of a login), check if we can cache
        if (login) {
            val storageService = DataStoreSource.storageService
            if (!storageService.canCache()) {
                DataStoreSource.colorLogger.warn("StorageService is not ready to cache objects, denying join")
                denyJoin = true
                joinDenyReason = Color.t(
                    DataStoreSource.config.getString("profiles.messages.beforeDbConnection")
                        .replace("{cacheName}", cache.name)
                )
                return null
            }
        }

        // Load details into this loader class
        try {
            this.store = loadOrCreateStore(cache, uuid, login, username)
        } catch (t: Throwable) {
            DataStoreFileLogger.warn("Failed to load or create StoreProfile from Database, denying join", t)
            this.denyJoin = true
            this.joinDenyReason = Color.t(
                DataStoreSource.config.getString("profiles.messages.beforeDbConnection")
                    .replace("{cacheName}", cache.name)
            )
        }

        // The above method will load the store into this variable if it exists
        val o: X? = this.store

        // Ensure the store is cached and has a valid cache reference
        o?.let { store ->
            if (saveToLocalCache) {
                cache.cache(store)
            } else {
                store.setCache(cache)
            }
        }
        return o
    }

    fun login(username: String) {
        this.login = true
        this.username = username
    }

    /**
     * Called in [ProfileListener.onProfileCachingInit]
     */
    fun initializeOnJoin(player: Player) {
        this.player = player
        if (store == null) {
            store = cache.getFromCache(player)
        }
        store?.initializePlayer(player)
    }

    companion object {
        fun <X : StoreProfile<X>> loadOrCreateStore(
            cache: StoreProfileCache<X>,
            uuid: UUID,
            creative: Boolean,
            username: String?
        ): X {
            // Try loading from local
            val localStore = cache.localStore[uuid]
            if (localStore != null) {
                return localStore
            }

            // Try loading from database
            val o = cache.databaseStore[uuid]
            if (o == null) {
                // Make a new profile if they are logging in
                if (creative) {
                    cache.getLoggerService().debug("Creating a new StoreProfile for: $username")
                    return createStore(
                        cache, uuid, username
                    ) { store: X -> store.setCache(cache) }
                }

                // Assume some other kind of failure:
                throw RuntimeException("Failed to load or create StoreProfile from Database")
            }

            // We have a valid store from Database
            val store: X = o
            store.setCache(cache)

            // For logins -> mark as loaded
            if (creative) {
                // Update their username
                if (username != null && store.getUsername() != username) {
                    // Attempt to save the new username
                    cache.update(store) { x: X -> x.usernameField.set(username) }
                }
            }
            return store
        }

        private fun <X : StoreProfile<X>> createStore(
            cache: ProfileCache<X>,
            uuid: UUID,
            username: String?,
            initializer: Consumer<X>
        ): X {
            try {
                // Create a new instance in modifiable state
                val store: X = cache.instantiator.instantiate()
                store.initialize()
                store.readOnly = false

                // Initialize the store
                initializer.accept(store)
                // Enforce Version 0 for creation
                store.idField.set(uuid)
                store.versionField.set(0L)
                store.usernameField.set(username)

                store.readOnly = true

                // Save the store to our database implementation & cache
                cache.cache(store)
                cache.databaseStore.save(store)
                return store
            } catch (e: Exception) {
                throw RuntimeException("Failed to create Store", e)
            }
        }
    }
}
