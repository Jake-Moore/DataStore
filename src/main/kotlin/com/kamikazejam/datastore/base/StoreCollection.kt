package com.kamikazejam.datastore.base

import com.google.common.base.Preconditions
import com.kamikazejam.datastore.DataStoreAPI
import com.kamikazejam.datastore.DataStoreRegistration
import com.kamikazejam.datastore.DataStoreSource
import com.kamikazejam.datastore.base.async.handler.crud.AsyncCreateHandler
import com.kamikazejam.datastore.base.async.handler.crud.AsyncDeleteHandler
import com.kamikazejam.datastore.base.async.handler.crud.AsyncReadHandler
import com.kamikazejam.datastore.base.async.handler.crud.AsyncUpdateHandler
import com.kamikazejam.datastore.base.async.handler.impl.AsyncHasKeyHandler
import com.kamikazejam.datastore.base.async.handler.impl.AsyncReadIdHandler
import com.kamikazejam.datastore.base.async.result.Empty
import com.kamikazejam.datastore.base.async.result.Failure
import com.kamikazejam.datastore.base.async.result.Success
import com.kamikazejam.datastore.base.exception.DuplicateCollectionException
import com.kamikazejam.datastore.base.field.FieldWrapper
import com.kamikazejam.datastore.base.field.OptionalField
import com.kamikazejam.datastore.base.field.RequiredField
import com.kamikazejam.datastore.base.index.IndexedField
import com.kamikazejam.datastore.base.log.LoggerService
import com.kamikazejam.datastore.base.store.CollectionLoggerInstantiator
import com.kamikazejam.datastore.base.store.StoreInstantiator
import com.kamikazejam.datastore.mode.profile.StoreProfileCollection
import com.kamikazejam.datastore.mode.profile.listener.ProfileListener
import com.mongodb.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
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
            store.readOnly = true
            return AsyncReadHandler(this) { store }
        }

        // Try Database Second
        return AsyncReadHandler(this) {
            val databaseStore = readFromDatabase(key)
            databaseStore?.let { store ->
                store.readOnly = true
                if (cacheStore) {
                    cache(store)
                }
                return@AsyncReadHandler store
            }
        }
    }

    @Throws(DuplicateKeyException::class)
    override fun create(key: K, initializer: Consumer<X>): AsyncCreateHandler<K, X> {
        Preconditions.checkNotNull(initializer, "Initializer cannot be null")

        return AsyncCreateHandler(this) {
            try {
                // Create a new instance in modifiable state
                val store: X = instantiator.instantiate()
                store.initialize()
                store.readOnly = false

                // Set the id first (allowing the initializer to change it if necessary)
                store.idField.setNotNull(key)
                // Initialize the store
                initializer.accept(store)
                // Enforce Version 0 for creation
                store.versionField.set(0L)

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

    override fun delete(key: K): AsyncDeleteHandler {
        return AsyncDeleteHandler(this) {
            localStore.remove(key)
            val removedFromDb = databaseStore.remove(key)
            invalidateIndexes(key, true)
            removedFromDb
        }
    }

    override fun update(key: K, updateFunction: Consumer<X>): AsyncUpdateHandler<K, X> {
        Preconditions.checkNotNull(updateFunction, "Update function cannot be null")

        return AsyncUpdateHandler(this) {
            when (val readResult = read(key).await()) {
                is Success -> {
                    val originalEntity = readResult.value
                    check(this.databaseStore.updateSync(originalEntity, updateFunction)) {
                        "[StoreCollection#update] Failed to update store with key: ${this.keyToString(key)}"
                    }

                    originalEntity
                }
                is Failure -> throw readResult.error
                is Empty -> throw NoSuchElementException("[StoreCollection#update] Store not found with key: ${this.keyToString(key)}")
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
            val db = databaseStore.get(key)
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
                // can't do much, just leave the value as is
                getLoggerService().warn("Update store didn't have a value for field: ${storeProvider.fieldWrapper.name}, class: ${storeProvider.fieldWrapper.getFieldType()}")
            }
        }

        store.readOnly = true
    }

    /**
     * Helper method to safely copy values between field wrappers of the same type
     */
    @Suppress("UNCHECKED_CAST")
    private fun copyFieldValue(target: FieldWrapper<*>, source: FieldWrapper<*>) {
        val targetAny = (target as FieldWrapper<Any>)
        val sourceAny = (source as FieldWrapper<Any>)
        if (targetAny is OptionalField<Any> && sourceAny is OptionalField<Any>) {
            targetAny.set(sourceAny.get())
        } else if (targetAny is RequiredField<Any> && sourceAny is RequiredField<Any>) {
            targetAny.set(sourceAny.get())
        } else {
            throw IllegalArgumentException(
                "FieldWrappers must be of the same type to copy values, found: "
                        + "${targetAny::class.java.simpleName} and ${sourceAny::class.java.simpleName}"
            )
        }
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

    override fun <T> readIdByIndex(index: IndexedField<X, T>, value: T): AsyncReadIdHandler<K> {
        return AsyncReadIdHandler(this) {
            DataStoreSource.storageService.getStoreIdByIndex(this, index, value)
        }
    }

    override fun <T> readByIndex(field: IndexedField<X, T>, value: T): AsyncReadHandler<K, X> {
        // 1. -> Check local cache (brute force)
        for (store in localStore.getAll()) {
            if (field.equals(field.getValue(store), value)) {
                return AsyncReadHandler(this) { store }
            }
        }

        // 2. -> Check database (uses storage service like mongodb)
        return AsyncReadHandler(this) {
            val id = DataStoreSource.storageService.getStoreIdByIndex(this, field, value) ?: return@AsyncReadHandler null

            // 3. -> Obtain the Profile by its ID
            when (val readResult = this.read(id).await()) {
                is Success -> {
                    if (!field.equals(value, field.getValue(readResult.value))) {
                        // This can happen if:
                        //    The local copy had its field changed
                        //    and those changes were not saved to DB or Index Cache
                        // This is not considered an error, but we should return empty
                        return@AsyncReadHandler null
                    }
                    return@AsyncReadHandler readResult.value
                }
                is Failure -> throw readResult.error
                is Empty -> return@AsyncReadHandler null
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

    override fun getKeyStringIdentifier(key: K): String {
        return keyToString(key) + "@" + this.name
    }
}
