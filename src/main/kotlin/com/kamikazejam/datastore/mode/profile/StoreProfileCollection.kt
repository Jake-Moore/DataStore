package com.kamikazejam.datastore.mode.profile

import com.google.common.base.Preconditions
import com.kamikazejam.datastore.DataStoreRegistration
import com.kamikazejam.datastore.DataStoreSource
import com.kamikazejam.datastore.base.Collection
import com.kamikazejam.datastore.base.StoreCollection
import com.kamikazejam.datastore.base.extensions.read
import com.kamikazejam.datastore.base.log.CollectionLoggerService
import com.kamikazejam.datastore.base.store.CollectionLoggerInstantiator
import com.kamikazejam.datastore.base.store.StoreInstantiator
import com.kamikazejam.datastore.event.profile.StoreProfileQuitEvent
import com.kamikazejam.datastore.mode.profile.store.ProfileStorageDatabase
import com.kamikazejam.datastore.mode.profile.store.ProfileStorageLocal
import com.kamikazejam.datastore.util.PlayerUtil
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

@Suppress("unused")
abstract class StoreProfileCollection<X : StoreProfile<X>> @JvmOverloads constructor(
    module: DataStoreRegistration,
    instantiator: StoreInstantiator<UUID, X>,
    name: String,
    storeClass: Class<X>,
    logger: CollectionLoggerInstantiator = CollectionLoggerInstantiator { collection: Collection<*, *> ->
        CollectionLoggerService(collection)
    }
) :
    StoreCollection<UUID, X>(instantiator, name, UUID::class.java, storeClass, module, logger),
    ProfileCollection<X> {
    private val loaders: ConcurrentMap<UUID, StoreProfileLoader<X>> = ConcurrentHashMap()
    override val localStore: ProfileStorageLocal<X> = ProfileStorageLocal()
    override val databaseStore: ProfileStorageDatabase<X> by lazy { ProfileStorageDatabase(this) }

    init {
        // Start this collection
        if (!start()) {
            // Data loss is not tolerated in DataStore, shutdown to prevent issues
            DataStoreSource.get().logger.severe("Failed to start Profile Collection: $name")
            Bukkit.shutdown()
        }
    }

    // ------------------------------------------------------ //
    //                  Collection Methods                    //
    // ------------------------------------------------------ //
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

    override fun readAllFromDatabase(cacheStores: Boolean): Iterable<X> {
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

    override fun getKeyType(): Class<UUID> {
        return UUID::class.java
    }

    override val cached: kotlin.collections.Collection<X>
        get() = localStore.localStorage.values

    override fun readFromCache(key: UUID): X? {
        return localStore.get(key)
    }

    override fun readFromDatabase(key: UUID, cacheStore: Boolean): X? {
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


    override suspend fun getOnline(): kotlin.collections.Collection<X> {
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

    override fun readFromDatabase(player: Player, cacheStore: Boolean): X? {
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
        val event = StoreProfileQuitEvent(player, this, profile)
        Bukkit.getPluginManager().callEvent(event)
    }
}
