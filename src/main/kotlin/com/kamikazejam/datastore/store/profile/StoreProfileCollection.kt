package com.kamikazejam.datastore.store.profile

import com.google.common.base.Preconditions
import com.kamikazejam.datastore.DataStoreRegistration
import com.kamikazejam.datastore.DataStoreSource
import com.kamikazejam.datastore.base.StoreCollection
import com.kamikazejam.datastore.base.async.handler.crud.AsyncCreateHandler
import com.kamikazejam.datastore.base.extensions.read
import com.kamikazejam.datastore.base.log.CollectionLoggerService
import com.kamikazejam.datastore.base.log.LoggerService
import com.kamikazejam.datastore.api.event.StoreProfileQuitEvent
import com.kamikazejam.datastore.store.profile.storage.ProfileStorageDatabase
import com.kamikazejam.datastore.store.profile.storage.ProfileStorageLocal
import com.kamikazejam.datastore.store.StoreProfile
import com.kamikazejam.datastore.util.PlayerJoinDetails
import com.kamikazejam.datastore.util.PlayerUtil
import com.mongodb.DuplicateKeyException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

@Suppress("unused")
abstract class StoreProfileCollection<X : StoreProfile<X>> @JvmOverloads constructor(
    module: DataStoreRegistration,
    override val instantiator: (UUID, Long, String?) -> X,
    name: String,
    storeClass: Class<X>,
    logger: (StoreCollection<UUID, X>) -> LoggerService = { store -> CollectionLoggerService(store) },
) :
    StoreCollection<UUID, X>(name, UUID::class.java, storeClass, module, logger),
    ProfileCollection<X> {
    private val loaders: ConcurrentMap<UUID, StoreProfileLoader<X>> = ConcurrentHashMap()
    override val localStore: ProfileStorageLocal<X> = ProfileStorageLocal()
    override val databaseStore: ProfileStorageDatabase<X> by lazy { ProfileStorageDatabase(this) }
    override val defaultInitializer: (X, PlayerJoinDetails) -> X = { store, _ -> store }

    init {
        // Start this collection
        val success = runBlocking { start() }
        if (!success) {
            // Data loss is not tolerated in DataStore, shutdown to prevent issues
            DataStoreSource.get().logger.severe("Failed to start Profile Collection: $name")
            Bukkit.shutdown()
        }
    }

    // ------------------------------------------------------ //
    //                  Collection Methods                    //
    // ------------------------------------------------------ //

    override fun initialize(): Boolean {
        Preconditions.checkNotNull(
            instantiator,
            "Instantiator must be set before calling start() for Collection $name"
        )
        return super.initialize()
    }

    override fun terminate(): Boolean {
        // Clear local stores (frees memory)
        loaders.clear()
        localStore.removeAll()

        // Don't clear database (can't)
        return true
    }

    // ----------------------------------------------------- //
    //                          CRUD                         //
    // ----------------------------------------------------- //
    @Suppress("DuplicatedCode")
    @Throws(DuplicateKeyException::class)
    override fun create(key: UUID, initializer: (X) -> X): AsyncCreateHandler<UUID, X> {
        Preconditions.checkNotNull(initializer, "Initializer cannot be null")

        return AsyncCreateHandler(this) {
            try {
                // Create a new instance in modifiable state
                val initial: X = instantiator(key, 0L, null)
                initial.initialize(this)

                // Initialize the store
                val store: X = initializer(initial)
                assert(store.id == key) { "Store ID must match key on Creation!" }
                assert(store.version == 0L) { "Store version must be 0 on Creation!" }

                // Save the store to our database implementation & cache
                // DO DATABASE SAVE FIRST SO ANY EXCEPTIONS ARE THROWN PRIOR TO MODIFYING LOCAL CACHE
                this.databaseStore.save(store)
                this.cache(store)
                return@AsyncCreateHandler store
            } catch (d: DuplicateKeyException) {
                getLoggerService().severe("Failed to create Store: Duplicate Key...")
                throw d
            } catch (t: Throwable) {
                // promote upwards, it will catch the errors
                throw t
            }
        }
    }

    override suspend fun readAllFromDatabase(cacheStores: Boolean): Flow<X> {
        return databaseStore.readAll()
    }

    // ----------------------------------------------------- //
    //                         Cache                         //
    // ----------------------------------------------------- //
    internal fun loader(key: UUID): StoreProfileLoader<X> {
        Preconditions.checkNotNull(key)
        return loaders.computeIfAbsent(key) { StoreProfileLoader(this) }
    }

    override fun keyToString(key: UUID): String {
        return key.toString()
    }

    override fun keyFromString(key: String): UUID {
        Preconditions.checkNotNull(key)
        return UUID.fromString(key)
    }

    override val cached: Collection<X>
        get() = localStore.localStorage.values

    override fun readFromCache(key: UUID): X? {
        return localStore.get(key)
    }

    override suspend fun readFromDatabase(key: UUID, cacheStore: Boolean): X? {
        val o: X? = databaseStore.read(key)
        if (cacheStore) {
            o?.let { store -> this.cache(store) }
        }
        return o
    }

    override val localCacheSize: Long
        get() = localStore.size()

    override suspend fun getIDs(): Flow<UUID> {
        return databaseStore.readKeys()
    }

    override suspend fun getOnline(): Collection<X> {
        return Bukkit.getOnlinePlayers()
            .filter { PlayerUtil.isFullyValidPlayer(it) }
            .mapNotNull { player ->
                this.read(player).await().getOrNull()
            }
    }

    override fun readFromCache(player: Player): X? {
        Preconditions.checkNotNull(player)
        return readFromCache(player.uniqueId)
    }

    override suspend fun readFromDatabase(player: Player, cacheStore: Boolean): X? {
        Preconditions.checkNotNull(player)
        val o: X? = databaseStore.read(player.uniqueId)
        if (cacheStore) {
            o?.let { store -> this.cache(store) }
        }
        return o
    }

    override fun removeLoader(uuid: UUID) {
        Preconditions.checkNotNull(uuid)
        loaders.remove(uuid)
    }

    override fun onProfileLeaving(player: Player, profile: X) {
        val event = StoreProfileQuitEvent(player, this, profile)
        Bukkit.getPluginManager().callEvent(event)
    }
}
