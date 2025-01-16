package com.kamikazejam.datastore.base

import com.google.common.base.Preconditions
import com.kamikazejam.datastore.DataStoreAPI
import com.kamikazejam.datastore.DataStoreRegistration
import com.kamikazejam.datastore.DataStoreSource
import com.kamikazejam.datastore.base.exception.DuplicateCacheException
import com.kamikazejam.datastore.base.field.FieldWrapper
import com.kamikazejam.datastore.base.index.IndexedField
import com.kamikazejam.datastore.base.log.LoggerService
import com.kamikazejam.datastore.base.store.CacheLoggerInstantiator
import com.kamikazejam.datastore.base.store.StoreInstantiator
import com.kamikazejam.datastore.mode.profile.StoreProfileCache
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
 * The abstract backbone of all Store cache systems.
 * All Caching modes (profile, object, simple) extend this class.
 */
abstract class StoreCache<K, X : Store<X, K>>(
    override var instantiator: StoreInstantiator<K, X>,
    override val name: String,
    protected val keyClass: Class<K>,
    override val storeClass: Class<X>,
    final override val registration: DataStoreRegistration,
    private val loggerInstantiator: CacheLoggerInstantiator
) : Comparable<StoreCache<*, *>>, Cache<K, X> {
    private val dependingCaches: MutableSet<String?> = HashSet()

    override val plugin: Plugin = registration.plugin

    protected var debug: Boolean = true
    override var running: Boolean = false

    init {
        // Make sure to load the Index Cache from disk when this cache is created
        this.saveIndexCache()
    }

    // ------------------------------------------------------ //
    // Generic CRUD Methods                                   //
    // ------------------------------------------------------ //
    override fun readSync(key: K, cacheStore: Boolean): X? {
        // Try Local Cache First
        val localStore = localStore.get(key)
        localStore?.let { store ->
            store.readOnly = true
            return store
        }

        // Try Database Second
        val databaseStore = databaseStore.get(key)
        databaseStore?.let { store ->
            store.readOnly = true
            if (cacheStore) {
                cache(store)
            }
            return store
        }

        return null
    }

    override fun readSync(key: K): X? {
        return this.readSync(key, true)
    }

    override fun readOrCreateSync(key: K, initializer: Consumer<X>): X {
        Preconditions.checkNotNull(initializer, "Initializer cannot be null")

        val o = readSync(key)
        return o ?: createSync(key, initializer)
    }

    @Throws(DuplicateKeyException::class)
    override fun createSync(key: K, initializer: Consumer<X>): X {
        Preconditions.checkNotNull(initializer, "Initializer cannot be null")

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
            return store
        } catch (d: DuplicateKeyException) {
            getLoggerService().severe("Failed to create Store: Duplicate Key...")
            throw d
        } catch (e: Exception) {
            throw RuntimeException("Failed to create Store", e)
        }
    }

    override fun deleteSync(key: K) {
        localStore.remove(key)
        databaseStore.remove(key)
        this.invalidateIndexes(key, true)
    }

    override fun deleteSync(store: X) {
        Preconditions.checkNotNull(store, "Store cannot be null")
        delete(store.id)
    }

    override fun updateSync(key: K, updateFunction: Consumer<X>): X {
        Preconditions.checkNotNull(updateFunction, "Update function cannot be null")

        val originalEntity = readSync(key) ?: throw NoSuchElementException("[StoreCache#update] Store not found with key: ${this.keyToString(key)}")

        check(
            this.databaseStore.updateSync(
                originalEntity,
                updateFunction
            )
        ) { "[StoreCache#update] Failed to update store with key: ${this.keyToString(key)}" }
        return originalEntity
    }

    // ------------------------------------------------------ //
    // Service Methods                                        //
    // ------------------------------------------------------ //
    /**
     * Start the Cache
     * Should be called by the external plugin during startup after the cache has been created
     *
     * @return Boolean successful
     */
    final override fun start(): Boolean {
        Preconditions.checkState(!running, "Cache $name is already started!")
        Preconditions.checkNotNull(
            instantiator,
            "Instantiator must be set before calling start() for cache $name"
        )
        var success = true
        if (!initialize()) {
            success = false
            getLoggerService().error("Failed to initialize internally for cache: $name")
        }
        running = true

        // Register this cache
        try {
            DataStoreAPI.saveCache(this)
        } catch (e: DuplicateCacheException) {
            getLoggerService().severe("[DuplicateCacheException] Failed to register cache: $name - Cache Name already exists!")
            return false
        }
        return success
    }

    /**
     * Stop the Cache
     * Should be called by the external plugin during shutdown
     *
     * @return Boolean successful
     */
    override fun shutdown(): Boolean {
        Preconditions.checkState(running, "Cache $name is not running!")
        var success = true

        // If this cache is a player cache, save all profiles of online players before we shut down
        if (this is StoreProfileCache<*>) {
            Bukkit.getOnlinePlayers().forEach { p: Player -> ProfileListener.quit(p, this) }
        }

        // terminate() handles the rest of the cache shutdown
        if (!terminate()) {
            success = false
            getLoggerService().info("Failed to terminate internally for cache: $name")
        }

        running = false

        // Unregister this cache
        DataStoreAPI.removeCache(this)
        return success
    }


    // ------------------------------------------------------ //
    // Cache Methods                                          //
    // ------------------------------------------------------ //
    /**
     * Starts up & initializes the cache.
     * Prepares everything for a fresh startup, ensures database connections, etc.
     */
    @ApiStatus.Internal
    protected abstract fun initialize(): Boolean

    /**
     * Shut down the cache.
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
        store.setCache(this)
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

    override fun addDepend(cache: Cache<*, *>) {
        Preconditions.checkNotNull(cache)
        dependingCaches.add(cache.name)
    }

    override fun isDependentOn(cache: Cache<*, *>): Boolean {
        Preconditions.checkNotNull(cache)
        return dependingCaches.contains(cache.name)
    }

    override fun isDependentOn(cacheName: String): Boolean {
        Preconditions.checkNotNull(cacheName)
        return dependingCaches.contains(cacheName)
    }

    /**
     * Simple comparator method to determine order between caches based on dependencies
     *
     * @param other The [StoreCache] to compare.
     * @return Comparator sorting integer
     */
    override fun compareTo(other: StoreCache<*, *>): Int {
        Preconditions.checkNotNull(other)
        return if (this.isDependentOn(other)) -1 else if (other.isDependentOn(this)) 1 else 0
    }

    override val dependencyNames: Set<String?>
        get() = dependingCaches

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

    override fun <T> getStoreIdByIndexSync(index: IndexedField<X, T>, value: T): K? {
        return DataStoreSource.storageService.getStoreIdByIndex(this, index, value)
    }

    override fun <T> getByIndexSync(field: IndexedField<X, T>, value: T): X? {
        // 1. -> Check local cache (brute force)
        for (store in localStore.all) {
            if (field.equals(field.getValue(store), value)) {
                return store
            }
        }

        // 2. -> Check database (uses cache or mongodb)
        val id = DataStoreSource.storageService.getStoreIdByIndex(this, field, value) ?: return null

        // 3. -> Obtain the Profile by its ID
        val o = this.readSync(id)
        if (o != null && !field.equals(value, field.getValue(o))) {
            // This can happen if:
            //    The local copy had its field changed
            //    and those changes were not saved to DB or Index Cache
            // This is not considered an error, but we should return empty
            return null
        }

        // Either the Optional is empty or the Store has the correct value -> return
        return o
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
