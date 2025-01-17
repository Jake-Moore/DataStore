package com.kamikazejam.datastore.mode.`object`

import com.google.common.base.Preconditions
import com.kamikazejam.datastore.DataStoreRegistration
import com.kamikazejam.datastore.DataStoreSource
import com.kamikazejam.datastore.base.Collection
import com.kamikazejam.datastore.base.StoreCollection
import com.kamikazejam.datastore.base.log.CollectionLoggerService
import com.kamikazejam.datastore.base.result.AsyncStoreHandler
import com.kamikazejam.datastore.base.store.CollectionLoggerInstantiator
import com.kamikazejam.datastore.base.store.StoreInstantiator
import com.kamikazejam.datastore.connections.storage.iterator.TransformingIterator
import com.kamikazejam.datastore.mode.`object`.store.ObjectStorageDatabase
import com.kamikazejam.datastore.mode.`object`.store.ObjectStorageLocal
import com.mongodb.*
import org.bukkit.Bukkit
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.function.Consumer

@Suppress("unused")
abstract class StoreObjectCollection<X : StoreObject<X>> @JvmOverloads constructor(
    module: DataStoreRegistration,
    instantiator: StoreInstantiator<String, X>,
    name: String,
    storeClass: Class<X>,
    logger: CollectionLoggerInstantiator = CollectionLoggerInstantiator { collection: Collection<*, *> -> CollectionLoggerService(collection) }
) :
    StoreCollection<String, X>(instantiator, name, String::class.java, storeClass, module, logger),
    ObjectCollection<X>
{
    private val loaders: ConcurrentMap<String, StoreObjectLoader<X>> = ConcurrentHashMap()
    override val localStore: ObjectStorageLocal<X> = ObjectStorageLocal()

    override val databaseStore: ObjectStorageDatabase<X> by lazy { ObjectStorageDatabase(this) }

    init {
        // Start this collection
        if (!start()) {
            // Data loss is not tolerated in DataStore, shutdown to prevent issues
            DataStoreSource.get().logger.severe("Failed to start Object Cache: $name")
            Bukkit.shutdown()
        }
    }

    // ------------------------------------------------------ //
    // CRUD Methods                                           //
    // ------------------------------------------------------ //
    @Throws(DuplicateKeyException::class)
    override fun create(initializer: Consumer<X>): AsyncStoreHandler<String, X> {
        return this.create(UUID.randomUUID().toString(), initializer)
    }

    // ------------------------------------------------------ //
    // Cache Methods                                          //
    // ------------------------------------------------------ //
    override fun initialize(): Boolean {
        // Nothing to do here
        return true
    }

    override fun terminate(): Boolean {
        // Don't save -> Stores are updated in their cache's update methods, they should not need to be saved here
        val success = true

        loaders.clear()
        // Clear local store (frees memory)
        localStore.clear()

        // Don't clear database (can't)
        return success
    }

    override fun loader(key: String): StoreObjectLoader<X> {
        Preconditions.checkNotNull(key)
        return loaders.computeIfAbsent(key) { s: String -> StoreObjectLoader(this, s) }
    }

    override fun keyToString(key: String): String {
        return key
    }

    override fun keyFromString(key: String): String {
        return key
    }

    override fun readAll(cacheStores: Boolean): Iterable<X> {
        val keysIterable = databaseStore.keys.iterator()

        // Create an Iterable that iterates through all database keys, transforming into Stores
        return Iterable {
            TransformingIterator(keysIterable) { key ->
                // 1. If we have the object in the cache -> return it
                val o: X? = localStore.get(key)
                if (o != null) {
                    return@TransformingIterator o
                }

                // 2. We don't have the object in the cache -> load it from the database
                // If for some reason this was deleted, or not found, we can just return null
                //  and the TransformingIterator will skip it
                val db: X = databaseStore.get(key) ?: return@TransformingIterator null

                // Optionally cache this loaded Store
                if (cacheStores) {
                    this.cache(db)
                }
                db
            }
        }
    }

    override fun readAllFromDatabase(cacheStores: Boolean): Iterable<X?> {
        // Create an Iterable that iterates through all database objects, and updates local objects as necessary
        val dbIterator: Iterator<X> = databaseStore.getAll(this).iterator()
        return Iterable {
            TransformingIterator(dbIterator) { dbStore: X ->
                // Load the local object
                val local: X? = localStore.get(dbStore.id)

                // If we want to cache, and have a local store that's newer -> update the local store
                // Note, if not caching then we won't update any local stores and won't cache the db store
                val dbVer = dbStore.versionField.get() ?: 0
                val localVer = local?.versionField?.get() ?: 0
                if (cacheStores && local != null && dbVer >= localVer) {
                    this@StoreObjectCollection.updateStoreFromNewer(local, dbStore)
                    this@StoreObjectCollection.cache(dbStore)
                }

                // Find the store object to return
                val ret = local ?: dbStore
                // Verify it has the correct cache and cache it if necessary
                ret.setCollection(this@StoreObjectCollection)
                ret
            }
        }
    }

    override val cached: kotlin.collections.Collection<X>
        get() = localStore.localStorage.values

    override fun hasKeySync(key: String): Boolean {
        return localStore.has(key) || databaseStore.has(key)
    }

    override fun readFromCache(key: String): X? {
        return localStore.get(key)
    }

    override fun readFromDatabase(key: String, cacheStore: Boolean): X? {
        val o: X? = databaseStore.get(key)
        if (cacheStore) {
            o?.let { this.cache(it) }
        }
        return o
    }

    override val localCacheSize: Long
        get() = localStore.size()

    override val iDs: Iterable<String>
        get() = databaseStore.keys
}
