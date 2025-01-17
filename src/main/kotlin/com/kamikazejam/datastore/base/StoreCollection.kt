package com.kamikazejam.datastore.base

import com.google.common.base.Preconditions
import com.kamikazejam.datastore.DataStoreAPI
import com.kamikazejam.datastore.DataStoreRegistration
import com.kamikazejam.datastore.DataStoreSource
import com.kamikazejam.datastore.base.exception.DuplicateCollectionException
import com.kamikazejam.datastore.base.field.FieldWrapper
import com.kamikazejam.datastore.base.index.IndexedField
import com.kamikazejam.datastore.base.log.LoggerService
import com.kamikazejam.datastore.base.result.AsyncHandler
import com.kamikazejam.datastore.base.result.CollectionResult
import com.kamikazejam.datastore.base.store.CollectionLoggerInstantiator
import com.kamikazejam.datastore.base.store.StoreInstantiator
import com.kamikazejam.datastore.connections.storage.iterator.TransformingIterator
import com.kamikazejam.datastore.mode.profile.StoreProfileCollection
import com.kamikazejam.datastore.mode.profile.listener.ProfileListener
import com.mongodb.*
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.IllegalPluginAccessException
import org.bukkit.plugin.Plugin
import org.jetbrains.annotations.ApiStatus
import java.util.*
import java.util.function.Consumer

/**
 * The abstract backbone of all Store Collection systems.
 * All Caching modes (profile, object, simple) extend this class.
 */
abstract class StoreCollection<K, X : Store<X, K>>(
    override var instantiator: StoreInstantiator<K, X>,
    override val name: String,
    protected val keyClass: Class<K>,
    override val storeClass: Class<X>,
    final override val registration: DataStoreRegistration,
    private val loggerInstantiator: CollectionLoggerInstantiator
) : Comparable<StoreCollection<*, *>>, Collection<K, X> {
    private val dependingCollections: MutableSet<String> = HashSet()

    override val plugin: Plugin = registration.plugin

    protected var debug: Boolean = true
    override var running: Boolean = false

    init {
        // Make sure to load the Index Cache from disk when this Collection is created
        this.saveIndexCache()
    }

    // ------------------------------------------------------ //
    // Generic CRUD Methods                                   //
    // ------------------------------------------------------ //
    override fun read(key: K, cacheStore: Boolean): AsyncHandler<X> {
        // Try Local Cache First
        val localStore = readFromCache(key)
        localStore?.let { store ->
            store.readOnly = true
            return AsyncHandler(this) { store }
        }

        // Try Database Second
        return AsyncHandler(this) {
            val databaseStore = readFromDatabase(key)
            databaseStore?.let { store ->
                store.readOnly = true
                if (cacheStore) {
                    cache(store)
                }
                return@AsyncHandler store
            }
        }
    }

    @Throws(DuplicateKeyException::class)
    override fun create(key: K, initializer: Consumer<X>): AsyncHandler<X> {
        Preconditions.checkNotNull(initializer, "Initializer cannot be null")

        return AsyncHandler(this) {
            try {
                // Create a new instance in modifiable state
                val store: X = instantiator.instantiate()
                store.initialize()
                store.readOnly = false

                // Set the id first (allowing the initializer to change it if necessary)
                store.idField.set(key)
                // Initialize the store
                initializer.accept(store)
                // Enforce Version 0 for creation
                store.versionField.set(0L)

                store.readOnly = true

                // Save the store to our database implementation & cache
                this.cache(store)
                this.databaseStore.save(store)
                return@AsyncHandler store
            } catch (d: DuplicateKeyException) {
                getLoggerService().severe("Failed to create Store: Duplicate Key...")
                throw d
            } catch (e: Exception) {
                throw RuntimeException("Failed to create Store", e)
            }
        }
    }

    override fun delete(key: K): AsyncHandler<Boolean> {
        localStore.remove(key)
        return AsyncHandler(this) {
            val removedFromDb = databaseStore.remove(key)
            invalidateIndexes(key, true)
            removedFromDb
        }
    }

    override fun update(key: K, updateFunction: Consumer<X>): AsyncHandler<X> {
        Preconditions.checkNotNull(updateFunction, "Update function cannot be null")

        return AsyncHandler(this) {
            when (val readResult = read(key).await()) {
                is CollectionResult.Success -> {
                    val originalEntity = readResult.value
                    check(this.databaseStore.updateSync(originalEntity, updateFunction)) {
                        "[StoreCollection#update] Failed to update store with key: ${this.keyToString(key)}"
                    }

                    originalEntity
                }
                is CollectionResult.Failure -> throw readResult.error
                is CollectionResult.Empty -> throw NoSuchElementException("[StoreCollection#update] Store not found with key: ${this.keyToString(key)}")
            }
        }
    }

    override fun readAll(cacheStores: Boolean): Iterable<X> {
        val keysIterable = iDs.iterator()

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

    // ------------------------------------------------------ //
    // Service Methods                                        //
    // ------------------------------------------------------ //
    /**
     * Start the Collection
     * Should be called by the external plugin during startup after the Collection has been created
     *
     * @return Boolean successful
     */
    final override fun start(): Boolean {
        Preconditions.checkState(!running, "Collection $name is already started!")
        Preconditions.checkNotNull(
            instantiator,
            "Instantiator must be set before calling start() for Collection $name"
        )
        var success = true
        if (!initialize()) {
            success = false
            getLoggerService().error("Failed to initialize internally for Collection: $name")
        }
        running = true

        // Register this Collection
        try {
            DataStoreAPI.saveCollection(this)
        } catch (e: DuplicateCollectionException) {
            getLoggerService().severe("[DuplicateCollectionException] Failed to register Collection: $name - Collection Name already exists!")
            return false
        }
        return success
    }

    /**
     * Stop the Collection
     * Should be called by the external plugin during shutdown
     *
     * @return Boolean successful
     */
    override fun shutdown(): Boolean {
        Preconditions.checkState(running, "Collection $name is not running!")
        var success = true

        // If this Collection is a player Collection, save all profiles of online players before we shut down
        if (this is StoreProfileCollection<*>) {
            Bukkit.getOnlinePlayers().forEach { p: Player -> ProfileListener.quit(p, this) }
        }

        // terminate() handles the rest of the Collection shutdown
        if (!terminate()) {
            success = false
            getLoggerService().info("Failed to terminate internally for Collection: $name")
        }

        running = false

        // Unregister this Collection
        DataStoreAPI.removeCollection(this)
        return success
    }


    // ------------------------------------------------------ //
    // Collection Methods                                     //
    // ------------------------------------------------------ //
    /**
     * Starts up & initializes the Collection.
     * Prepares everything for a fresh startup, ensures database connections, etc.
     */
    @ApiStatus.Internal
    protected abstract fun initialize(): Boolean

    /**
     * Shut down the Collection.
     * Saves everything first, and safely shuts down
     */
    @ApiStatus.Internal
    protected abstract fun terminate(): Boolean

    override val databaseName: String
        get() = registration.databaseName

    override fun cache(store: X) {
        Preconditions.checkNotNull(store)
        val cached = localStore[store.id]
        if (cached != null) {
            // If the objects are different -> update the one in the cache
            //   Note: this is not an equality check, this is a reference check (as intended)
            if (cached !== store) {
                updateStoreFromNewer(cached, store)
            }
        } else {
            localStore.save(store)
            getLoggerService().debug("Cached store " + store.id)
        }
        store.setCollection(this)
    }

    override fun uncache(key: K) {
        localStore.remove(key)
    }

    override fun uncache(store: X) {
        Preconditions.checkNotNull(store)
        localStore.remove(store)
    }

    override fun isCached(key: K): Boolean {
        return localStore.has(key)
    }

    override fun runAsync(runnable: Runnable) {
        Preconditions.checkNotNull(runnable)
        plugin.server.scheduler.runTaskAsynchronously(plugin, runnable)
    }

    override fun runSync(runnable: Runnable) {
        Preconditions.checkNotNull(runnable)
        plugin.server.scheduler.runTask(plugin, runnable)
    }

    override fun tryAsync(runnable: Runnable) {
        try {
            runAsync(runnable)
        } catch (e: IllegalPluginAccessException) {
            runnable.run()
        }
    }

    override fun addDepend(collection: Collection<*, *>) {
        Preconditions.checkNotNull(collection)
        dependingCollections.add(collection.name)
    }

    override fun isDependentOn(collection: Collection<*, *>): Boolean {
        Preconditions.checkNotNull(collection)
        return dependingCollections.contains(collection.name)
    }

    override fun isDependentOn(collName: String): Boolean {
        Preconditions.checkNotNull(collName)
        return dependingCollections.contains(collName)
    }

    /**
     * Simple comparator method to determine order between collections based on dependencies
     *
     * @param other The [StoreCollection] to compare.
     * @return Comparator sorting integer
     */
    override fun compareTo(other: StoreCollection<*, *>): Int {
        Preconditions.checkNotNull(other)
        return if (this.isDependentOn(other)) -1 else if (other.isDependentOn(this)) 1 else 0
    }

    override val dependencyNames: Set<String>
        get() = dependingCollections

    @ApiStatus.Internal
    override fun updateStoreFromNewer(store: X, update: X) {
        Preconditions.checkNotNull(store)
        Preconditions.checkNotNull(update)

        // For this, we will simply go through the store's fields and set them to the update's fields values
        // This is a simple way to update the store from the update/
        // This is likely impossible to do in a fully type-safe way, but assuming both stores are the same type, this should work
        store.readOnly = false
        val storeFields = store.allFieldsMap
        val updateFields = update.allFieldsMap
        
        // Copy fields using type-safe helper method
        for ((key, storeProvider) in storeFields) {
            val updateProvider = updateFields[key]
            if (updateProvider != null) {
                copyFieldValue(storeProvider.fieldWrapper, updateProvider.fieldWrapper)
            }else {
                // For some reason our update didn't have a value for this field
                // Set the field to null
                storeProvider.fieldWrapper.set(null)
                getLoggerService().warn("Update store didn't have a value for field: ${storeProvider.fieldWrapper.name}, class: ${storeProvider.fieldWrapper.getValueType()}")
            }
        }

        store.readOnly = true
    }

    /**
     * Helper method to safely copy values between field wrappers of the same type
     */
    @Suppress("UNCHECKED_CAST")
    private fun copyFieldValue(target: FieldWrapper<*>, source: FieldWrapper<*>) {
        (target as FieldWrapper<Any>).set(source.get())
    }

    // ------------------------------------------------- //
    //                     Indexing                      //
    // ------------------------------------------------- //
    override fun <T> registerIndex(field: IndexedField<X, T>): IndexedField<X, T> {
        getLoggerService().debug("Registering index: " + field.name)
        DataStoreSource.storageService.registerIndex(this, field)
        return field
    }

    override fun cacheIndexes(store: X, save: Boolean) {
        DataStoreSource.storageService.cacheIndexes(this, store, save)
    }

    override fun invalidateIndexes(key: K, save: Boolean) {
        DataStoreSource.storageService.invalidateIndexes(this, key, save)
    }

    override fun saveIndexCache() {
        DataStoreSource.storageService.saveIndexCache(this)
    }

    override fun <T> readIdByIndex(index: IndexedField<X, T>, value: T): AsyncHandler<K> {
        return AsyncHandler(this) {
            DataStoreSource.storageService.getStoreIdByIndex(this, index, value)
        }
    }

    override fun <T> readByIndex(field: IndexedField<X, T>, value: T): AsyncHandler<X> {
        // 1. -> Check local cache (brute force)
        for (store in localStore.all) {
            if (field.equals(field.getValue(store), value)) {
                return AsyncHandler(this) { store }
            }
        }

        // 2. -> Check database (uses storage service like mongodb)
        return AsyncHandler(this) {
            val id = DataStoreSource.storageService.getStoreIdByIndex(this, field, value) ?: return@AsyncHandler null

            // 3. -> Obtain the Profile by its ID
            when (val readResult = this.read(id).await()) {
                is CollectionResult.Success -> {
                    if (!field.equals(value, field.getValue(readResult.value))) {
                        // This can happen if:
                        //    The local copy had its field changed
                        //    and those changes were not saved to DB or Index Cache
                        // This is not considered an error, but we should return empty
                        return@AsyncHandler null
                    }
                    return@AsyncHandler readResult.value
                }
                is CollectionResult.Failure -> throw readResult.error
                is CollectionResult.Empty -> return@AsyncHandler null
            }
        }
    }

    private var _loggerService: LoggerService? = null
    override fun getLoggerService(): LoggerService {
        val service = _loggerService
        if (service != null) {
            return service
        }
        val s = loggerInstantiator.instantiate(this)
        this._loggerService = s
        return s
    }

    override fun setLoggerService(loggerService: LoggerService) {
        _loggerService = loggerService
    }
}
