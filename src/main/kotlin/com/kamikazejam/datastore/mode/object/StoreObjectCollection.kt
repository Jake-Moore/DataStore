package com.kamikazejam.datastore.mode.`object`

import com.google.common.base.Preconditions
import com.kamikazejam.datastore.DataStoreRegistration
import com.kamikazejam.datastore.DataStoreSource
import com.kamikazejam.datastore.base.Collection
import com.kamikazejam.datastore.base.StoreCollection
import com.kamikazejam.datastore.base.async.handler.crud.AsyncCreateHandler
import com.kamikazejam.datastore.base.log.CollectionLoggerService
import com.kamikazejam.datastore.base.store.CollectionLoggerInstantiator
import com.kamikazejam.datastore.base.store.StoreInstantiator
import com.kamikazejam.datastore.mode.`object`.store.ObjectStorageDatabase
import com.kamikazejam.datastore.mode.`object`.store.ObjectStorageLocal
import com.mongodb.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
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
    // Collection Methods                                     //
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
        localStore.removeAll()

        // Don't clear database (can't)
        return success
    }

    // ----------------------------------------------------- //
    //                          CRUD                         //
    // ----------------------------------------------------- //
    @Throws(DuplicateKeyException::class)
    override fun create(key: String, initializer: Consumer<X>): AsyncCreateHandler<String, X> {
        Preconditions.checkNotNull(initializer, "Initializer cannot be null")

        return AsyncCreateHandler(this) {
            try {
                // Create a new instance in modifiable state
                val store: X = instantiator.instantiate()
                store.initialize()
                store.readOnly = false

                // Set the id first (allowing the initializer to change it if necessary)
                store.idField.getData().set(key)
                // Initialize the store
                initializer.accept(store)
                // Enforce Version 0 for creation
                store.versionField.getData().set(0L)

                store.readOnly = true

                // Save the store to our database implementation & cache
                // DO DATABASE SAVE FIRST SO ANY EXCEPTIONS ARE THROWN PRIOR TO MODIFYING LOCAL CACHE
                this.databaseStore.save(store)
                this.cache(store)
                return@AsyncCreateHandler store
            } catch (d: DuplicateKeyException) {
                getLoggerService().severe("Failed to create Store: Duplicate Key...")
                throw d
            } catch (e: Exception) {
                // promote upwards, it will catch the errors
                throw e
            }
        }
    }

    @Throws(DuplicateKeyException::class)
    override fun create(initializer: Consumer<X>): AsyncCreateHandler<String, X> {
        return this.create(UUID.randomUUID().toString(), initializer)
    }

    // ----------------------------------------------------- //
    //                 Misc Collection Methods               //
    // ----------------------------------------------------- //
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

    override suspend fun readAllFromDatabase(cacheStores: Boolean): Flow<X> = flow {
        // Create an Iterable that iterates through all database objects, and updates local objects as necessary
        databaseStore.getAll(this@StoreObjectCollection).map { dbStore: X ->
            // Load the local object
            val local: X? = localStore.get(dbStore.id)

            // If we want to cache, and have a local store that's newer -> update the local store
            // Note, if not caching then we won't update any local stores and won't cache the db store
            val dbVer = dbStore.versionField.getData().get()
            val localVer = local?.versionField?.getData()?.get() ?: 0
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

    override val cached: kotlin.collections.Collection<X>
        get() = localStore.localStorage.values

    override fun readFromCache(key: String): X? {
        return localStore.get(key)
    }

    override suspend fun readFromDatabase(key: String, cacheStore: Boolean): X? {
        val o: X? = databaseStore.get(key)
        if (cacheStore) {
            o?.let { this.cache(it) }
        }
        return o
    }

    override val localCacheSize: Long
        get() = localStore.size()

    override suspend fun getIDs(): Flow<String> {
        return databaseStore.getKeys()
    }
}
