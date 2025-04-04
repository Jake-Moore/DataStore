package com.kamikazejam.datastore.base

import com.google.common.base.Preconditions
import com.kamikazejam.datastore.DataStoreAPI
import com.kamikazejam.datastore.DataStoreRegistration
import com.kamikazejam.datastore.DataStoreSource
import com.kamikazejam.datastore.base.async.handler.crud.AsyncDeleteHandler
import com.kamikazejam.datastore.base.async.handler.crud.AsyncReadHandler
import com.kamikazejam.datastore.base.async.handler.crud.AsyncRejectableUpdateHandler
import com.kamikazejam.datastore.base.async.handler.crud.AsyncUpdateHandler
import com.kamikazejam.datastore.base.async.handler.impl.AsyncHasKeyHandler
import com.kamikazejam.datastore.base.async.handler.impl.AsyncReadIdHandler
import com.kamikazejam.datastore.base.async.result.Empty
import com.kamikazejam.datastore.base.async.result.Failure
import com.kamikazejam.datastore.base.async.result.Success
import com.kamikazejam.datastore.base.exception.DuplicateCollectionException
import com.kamikazejam.datastore.base.index.IndexedField
import com.kamikazejam.datastore.base.log.LoggerService
import com.kamikazejam.datastore.base.metrics.MetricsListener
import com.kamikazejam.datastore.store.Store
import com.kamikazejam.datastore.store.profile.StoreProfileCollection
import com.kamikazejam.datastore.store.profile.listener.ProfileListener
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.IllegalPluginAccessException
import org.bukkit.plugin.Plugin
import org.jetbrains.annotations.ApiStatus

/**
 * The abstract backbone of all Store Collection systems.
 * All Caching modes (profile, object, simple) extend this class.
 */
abstract class StoreCollection<K : Any, X : Store<X, K>>(
    override val name: String,
    protected val keyClass: Class<K>,
    override val storeClass: Class<X>,
    final override val registration: DataStoreRegistration,
    private val loggerInstantiator: (StoreCollection<K, X>) -> LoggerService
) : Comparable<StoreCollection<*, *>>, Collection<K, X> {
    private val dependingCollections: MutableSet<String> = HashSet()

    override val plugin: Plugin = registration.plugin

    protected var debug: Boolean = true
    override var running: Boolean = false

    init {
        // Make sure to load the Index Cache from disk when this Collection is created
        // This should occur on start up, so it's okay to block here
        runBlocking { saveIndexCache() }
    }

    // ------------------------------------------------------ //
    // Generic CRUD Methods                                   //
    // ------------------------------------------------------ //
    override fun read(key: K, cacheStore: Boolean): AsyncReadHandler<K, X> {
        // Try Local Cache First
        val localStore = readFromCache(key)
        localStore?.let { store ->
            return AsyncReadHandler(this) { store }
        }

        // Try Database Second
        return AsyncReadHandler(this) {
            val databaseStore = readFromDatabase(key)
            databaseStore?.let { store ->
                if (cacheStore) {
                    cache(store)
                }
                return@AsyncReadHandler store
            }
        }
    }

    override fun delete(key: K): AsyncDeleteHandler {
        return AsyncDeleteHandler(this) {
            localStore.remove(key)
            val removedFromDb = databaseStore.delete(key)
            invalidateIndexes(key, true)
            removedFromDb
        }
    }

    override fun update(key: K, updateFunction: (X) -> X): AsyncUpdateHandler<K, X> {
        Preconditions.checkNotNull(updateFunction, "Update function cannot be null")

        // Use the Success/Failure wrapper for the update
        return AsyncUpdateHandler(this) {
            return@AsyncUpdateHandler updateInner(key, updateFunction)
        }
    }

    override fun updateRejectable(key: K, updateFunction: (X) -> X): AsyncRejectableUpdateHandler<K, X> {
        Preconditions.checkNotNull(updateFunction, "Update function cannot be null")

        // Use the Success/Failure/Reject wrapper for the update
        return AsyncRejectableUpdateHandler(this) {
            return@AsyncRejectableUpdateHandler updateInner(key, updateFunction)
        }
    }

    private suspend fun updateInner(key: K, updateFunction: (X) -> X): X {
        when (val readResult = read(key).await()) {
            is Success -> {
                // may throw UpdateException, which will be caught by AsyncUpdateHandler
                return this.databaseStore.update(readResult.value, updateFunction)
            }
            is Failure -> throw readResult.error
            is Empty -> {
                DataStoreSource.metricsListeners.forEach(MetricsListener::onUpdateFailNotFound)
                throw NoSuchElementException("[StoreCollection#update] Store not found with key: ${this.keyToString(key)}")
            }
        }
    }

    override suspend fun readAll(cacheStores: Boolean): Flow<X> = flow {
        getIDs().collect { key ->
            // 1. If we have the object in the cache -> emit it
            val cached = localStore.get(key)
            if (cached != null) {
                emit(cached)
                return@collect
            }

            // 2. We don't have the object in the cache -> load it from the database
            val db = databaseStore.read(key)
            if (db != null) {
                // Optionally cache this loaded Store
                if (cacheStores) {
                    cache(db)
                }
                emit(db)
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
    final override suspend fun start(): Boolean {
        Preconditions.checkState(!running, "Collection $name is already started!")
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
    override suspend fun shutdown(): Boolean {
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
    protected open fun initialize(): Boolean = true

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
        val cached = localStore.get(store.id)
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
        store.initialize(this)
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

    final override fun hasKey(key: K): AsyncHasKeyHandler {
        return AsyncHasKeyHandler(this) {
            localStore.has(key) || databaseStore.has(key)
        }
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

        // All Store Objects should be read-only with val properties
        // Thus, to "swap" the active store with the update, we simply need to update our local cache
        // There is no concept of "hot swapping" stores in this DataStore library
        this.uncache(store.id)
        this.cache(update)
    }

    // ------------------------------------------------- //
    //                     Indexing                      //
    // ------------------------------------------------- //
    override suspend fun <T> registerIndex(field: IndexedField<X, T>): IndexedField<X, T> {
        getLoggerService().debug("Registering index: " + field.name)
        DataStoreSource.storageService.registerIndex(this, field)
        return field
    }

    override suspend fun cacheIndexes(store: X, save: Boolean) {
        DataStoreSource.storageService.cacheIndexes(this, store, save)
    }

    override suspend fun invalidateIndexes(key: K, save: Boolean) {
        DataStoreSource.storageService.invalidateIndexes(this, key, save)
    }

    override suspend fun saveIndexCache() {
        DataStoreSource.storageService.saveIndexCache(this)
    }

    override fun <T> readIdByIndex(field: IndexedField<X, T>, value: T): AsyncReadIdHandler<K> {
        // 1. -> Check local cache (brute force)
        val localStore = readFromCacheByIndex(field, value)
        if (localStore != null) return AsyncReadIdHandler(this) { localStore.id }

        // 2. -> Check database (uses storage service like mongodb)
        return AsyncReadIdHandler(this) {
            DataStoreSource.storageService.readStoreByIndex(this, field, value)?.id
        }
    }

    override fun <T> readByIndex(field: IndexedField<X, T>, value: T): AsyncReadHandler<K, X> {
        // 1. -> Check local cache (brute force)
        val localStore = readFromCacheByIndex(field, value)
        if (localStore != null) return AsyncReadHandler(this) { localStore }

        // 2. -> Check database (uses storage service like mongodb)
        return AsyncReadHandler(this) {
            DataStoreSource.storageService.readStoreByIndex(this, field, value)
        }
    }

    override fun <T> readFromCacheByIndex(field: IndexedField<X, T>, value: T): X? {
        return localStore.getAll().stream().filter { field.equals(field.getValue(it), value) }
            .findFirst()
            .orElse(null)
    }

    private var _loggerService: LoggerService? = null
    override fun getLoggerService(): LoggerService {
        val service = _loggerService
        if (service != null) {
            return service
        }
        val s = loggerInstantiator(this)
        this._loggerService = s
        return s
    }

    override fun setLoggerService(loggerService: LoggerService) {
        _loggerService = loggerService
    }

    override fun getKeyStringIdentifier(key: K): String {
        return keyToString(key) + "@" + this.name
    }
}
