package com.kamikazejam.datastore.mode.profile

import com.google.common.base.Preconditions
import com.kamikazejam.datastore.DataStoreRegistration
import com.kamikazejam.datastore.DataStoreSource
import com.kamikazejam.datastore.base.Cache
import com.kamikazejam.datastore.base.StoreCache
import com.kamikazejam.datastore.base.log.CacheLoggerService
import com.kamikazejam.datastore.base.store.CacheLoggerInstantiator
import com.kamikazejam.datastore.base.store.StoreInstantiator
import com.kamikazejam.datastore.connections.storage.iterator.TransformingIterator
import com.kamikazejam.datastore.event.profile.StoreProfileCacheQuitEvent
import com.kamikazejam.datastore.mode.profile.store.ProfileStorageDatabase
import com.kamikazejam.datastore.mode.profile.store.ProfileStorageLocal
import com.kamikazejam.datastore.util.PlayerUtil
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.stream.Collectors

@Suppress("unused")
abstract class StoreProfileCache<X : StoreProfile<X>> @JvmOverloads constructor(
    module: DataStoreRegistration,
    instantiator: StoreInstantiator<UUID, X>,
    name: String,
    storeClass: Class<X>,
    logger: CacheLoggerInstantiator = CacheLoggerInstantiator { cache: Cache<*, *> ->
        CacheLoggerService(cache)
    }
) :
    StoreCache<UUID, X>(instantiator, name, UUID::class.java, storeClass, module, logger),
    ProfileCache<X> {
    private val loaders: ConcurrentMap<UUID, StoreProfileLoader<X>> = ConcurrentHashMap()
    override val localStore: ProfileStorageLocal<X> = ProfileStorageLocal()
    override val databaseStore: ProfileStorageDatabase<X> by lazy { ProfileStorageDatabase(this) }

    init {
        // Start this cache
        if (!start()) {
            // Data loss is not tolerated in DataStore, shutdown to prevent issues
            DataStoreSource.get().logger.severe("Failed to start Profile Cache: $name")
            Bukkit.shutdown()
        }
    }

    override fun initialize(): Boolean {
        // nothing to do here
        return true
    }

    override fun terminate(): Boolean {
        loaders.clear()
        // Clear locals store (frees memory)
        localStore.clear()

        // Don't clear database (can't)
        return true
    }

    // ----------------------------------------------------- //
    //                          CRUD                         //
    // ----------------------------------------------------- //
    override fun readSync(player: Player, cacheStore: Boolean): X {
        val store: X = StoreProfileLoader.loadOrCreateStore(this, player.uniqueId, true, player.name)
        if (cacheStore) {
            this.cache(store)
        }
        return store
    }

    override fun readAll(cacheStores: Boolean): Iterable<X> {
        return Iterable {
            TransformingIterator(iDs.iterator()) { id: UUID ->
                readSync(id, cacheStores)
            }
        }
    }

    override fun readAllFromDatabase(cacheStores: Boolean): Iterable<X?> {
        return databaseStore.all
    }

    // ----------------------------------------------------- //
    //                         Cache                         //
    // ----------------------------------------------------- //
    override fun loader(key: UUID): StoreProfileLoader<X> {
        Preconditions.checkNotNull(key)
        return loaders.computeIfAbsent(key) { s: UUID -> StoreProfileLoader(this, s) }
    }

    override fun keyToString(key: UUID): String {
        return key.toString()
    }

    override fun keyFromString(key: String): UUID {
        Preconditions.checkNotNull(key)
        return UUID.fromString(key)
    }

    override val cached: Collection<X>
        get() = localStore.localCache.values

    override fun hasKeySync(key: UUID): Boolean {
        return localStore.has(key) || databaseStore.has(key)
    }

    override fun getFromCache(key: UUID): X? {
        return localStore.get(key)
    }

    override fun getFromDatabase(key: UUID, cacheStore: Boolean): X? {
        val o: X? = databaseStore.get(key)
        if (cacheStore) {
            o?.let { store -> this.cache(store) }
        }
        return o
    }

    override val localCacheSize: Long
        get() = localStore.size()

    override val iDs: Iterable<UUID>
        get() = databaseStore.keys


    override val online: Collection<X>
        // ----------------------------------------------------- //
        get() =// Stream online players and map them to their StoreProfile
            Bukkit.getOnlinePlayers().stream()
                .filter { PlayerUtil.isFullyValidPlayer(it) }
                .map { player: Player -> this.readSync(player) }
                .collect(Collectors.toSet())

    override fun getFromCache(player: Player): X? {
        Preconditions.checkNotNull(player)
        return getFromCache(player.uniqueId)
    }

    override fun getFromDatabase(player: Player, cacheStore: Boolean): X? {
        Preconditions.checkNotNull(player)
        val o: X? = databaseStore.get(player.uniqueId)
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
        val event = StoreProfileCacheQuitEvent(player, this, profile)
        Bukkit.getPluginManager().callEvent(event)
    }
}
