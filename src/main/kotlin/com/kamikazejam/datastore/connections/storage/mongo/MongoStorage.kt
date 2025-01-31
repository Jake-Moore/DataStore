package com.kamikazejam.datastore.connections.storage.mongo

import com.google.common.base.Preconditions
import com.kamikazejam.datastore.DataStoreSource
import com.kamikazejam.datastore.base.Collection
import com.kamikazejam.datastore.base.StoreCollection
import com.kamikazejam.datastore.base.exception.update.UpdateException
import com.kamikazejam.datastore.base.index.IndexedField
import com.kamikazejam.datastore.base.serialization.SerializationUtil.getSerialName
import com.kamikazejam.datastore.connections.config.MongoConfig
import com.kamikazejam.datastore.connections.monitor.MongoMonitor
import com.kamikazejam.datastore.connections.storage.StorageService
import com.kamikazejam.datastore.store.Store
import com.kamikazejam.datastore.util.DataStoreFileLogger
import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.MongoException
import com.mongodb.MongoTimeoutException
import com.mongodb.ServerAddress
import com.mongodb.WriteConcern
import com.mongodb.client.model.Filters
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Projections
import com.mongodb.connection.ClusterSettings
import com.mongodb.connection.ServerDescription
import com.mongodb.connection.ServerSettings
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import org.bson.Document
import org.bson.UuidRepresentation
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask
import java.util.concurrent.ConcurrentHashMap

@Suppress("unused")
class MongoStorage : StorageService() {
    override var running = false

    var mongoInitConnect = false

    var mongoConnected = false
    private val serverPingMap: MutableMap<ServerAddress, Long> = HashMap()
    override var averagePingNanos: Long = 1_000_000 // Default to 1ms (is updated every cluster and heartbeat event)
        private set

    // MongoDB
    var mongoClient: MongoClient? = null

    // ------------------------------------------------- //
    //                     Service                       //
    // ------------------------------------------------- //
    override suspend fun start(): Boolean {
        this.debug("Connecting to MongoDB")

        val mongo = this.connectMongo()
        this.running = true

        if (!mongo) {
            this.error("Failed to start MongoStorage, connection failed.")
            return false
        }
        // Client was created, MongoMonitor should log more as the connection succeeds or fails
        return true
    }

    override suspend fun shutdown(): Boolean {
        // If not running, warn and return true (we are already shutdown)
        if (!running) {
            this.warn("MongoStorage.shutdown() called while service is not running!")
            return true
        }

        // Disconnect from MongoDB
        val mongo = this.disconnectMongo()
        this.running = false

        if (!mongo) {
            this.error("Failed to shutdown MongoStorage, disconnect failed.")
            return false
        }

        this.debug("Disconnected from MongoDB")
        return true
    }

    // ------------------------------------------------- //
    //                StorageService                     //
    // ------------------------------------------------- //
    override suspend fun <K : Any, X : Store<X, K>> get(collection: Collection<K, X>, key: K): X? = withContext(Dispatchers.IO) {
        try {
            // MongoDB! Kotlin Driver w/ Serialization! We already have the Store Class we want.
            val idField = getSerialName(Store<X, K>::id)
            val store = getMongoCollection(collection).find(
                // Find the id field, where the content is the id string
                Filters.eq(idField, collection.keyToString(key))
            ).firstOrNull() ?: return@withContext null

            // Cache Indexes since we are loading from database
            collection.cacheIndexes(store, true)
            return@withContext store
        } catch (ex: MongoException) {
            collection.getLoggerService().info(ex, "MongoDB error getting Store (${collection.getKeyStringIdentifier(key)}) from MongoDB Layer")
            return@withContext null
        } catch (expected: Exception) {
            collection.getLoggerService().info(expected, "Error getting Store (${collection.getKeyStringIdentifier(key)}) from MongoDB Layer")
            return@withContext null
        }
    }

    override suspend fun <K : Any, X : Store<X, K>> save(collection: Collection<K, X>, store: X): Boolean = withContext(Dispatchers.IO) {
        // Sanity Check, our "id" field should be serializing as "_id" in MongoDB (otherwise ERROR!)
        if (getSerialName(Store<X, K>::id) != "_id") {
            throw IllegalStateException("MongoDB requires the ID field to be serialized as '_id'! Ensure your Store.id property is annotated with @SerialName(\"_id\")")
        }

        // Save to database with a transaction & only 1 attempt
        val client = mongoClient ?: throw IllegalStateException("MongoClient is not initialized!")
        client.startSession().use { session ->
            session.startTransaction()
            var committed = false
            try {
                getMongoCollection(collection).insertOne(session, store)
                session.commitTransaction()
                committed = true

                // Convert to read-only and cache
                collection.cache(store)
            } finally {
                if (!committed) {
                    session.abortTransaction()
                }
            }
        }
        return@withContext true
    }

    // This method needs no additional concurrency safeguards, because of the transactional nature of the update
    //  and the compare-and-swap design of the mongo query & its filters.
    // If two threads enter this method for the same object, only one will succeed in updating the object.
    //  and the other will have its query fail and will automatically retry.
    // (MongoDB provides the document-level locking already)
    @Throws(UpdateException::class)
    override suspend fun <K : Any, X : Store<X, K>> updateSync(
        collection: Collection<K, X>,
        store: X,
        updateFunction: (X) -> X
    ): X = withContext(Dispatchers.IO) {
        // we promote all exceptions upwards, for the AsyncUpdateHandler to manage/catch
        try {
            val client = mongoClient ?: throw IllegalStateException("MongoClient is not initialized!")
            val mongoColl = getMongoCollection(collection).withWriteConcern(WriteConcern.MAJORITY) // Ensure replication
            return@withContext MongoTransactionHelper.executeUpdate(client, mongoColl, collection, store, updateFunction)
        } catch (uE: UpdateException) {
            throw uE
        } catch (e: Exception) {
            DataStoreFileLogger.warn("Failed to update Store in MongoDB Layer after all retries: " + store.id, e)
            throw UpdateException("Failed to update Store in MongoDB Layer after all retries: " + store.id, e)
        }
    }

    override suspend fun <K : Any, X : Store<X, K>> has(collection: Collection<K, X>, key: K): Boolean = withContext(Dispatchers.IO) {
        try {
            // Filter based on the dot notation, since we know all ID Fields are SimpleStoreData, where the content is the id string
            val idField = getSerialName(Store<X, K>::id)
            val filter = Filters.eq(idField, collection.keyToString(key))
            return@withContext getMongoCollection(collection).countDocuments(filter) > 0
        } catch (ex: MongoException) {
            collection.getLoggerService().info(ex, "MongoDB error check if Store (${collection.getKeyStringIdentifier(key)}) exists in MongoDB Layer")
            return@withContext false
        } catch (expected: Exception) {
            collection.getLoggerService().info(expected, "Error checking if Store (${collection.getKeyStringIdentifier(key)}) exists in MongoDB Layer")
            return@withContext false
        }
    }

    override suspend fun <K : Any, X : Store<X, K>> size(collection: Collection<K, X>): Long = withContext(Dispatchers.IO) {
        return@withContext getMongoCollection(collection).countDocuments()
    }

    override suspend fun <K : Any, X : Store<X, K>> remove(collection: Collection<K, X>, key: K): Boolean = withContext(Dispatchers.IO) {
        try {
            // Filter based on the dot notation, since we know all ID Fields are SimpleStoreData, where the content is the id string
            val idField = getSerialName(Store<X, K>::id)
            val filter = Filters.eq(idField, collection.keyToString(key))
            return@withContext getMongoCollection(collection).deleteMany(filter).deletedCount > 0
        } catch (ex: MongoException) {
            collection.getLoggerService().info(ex, "MongoDB error removing Store (${collection.getKeyStringIdentifier(key)}) from MongoDB Layer")
        } catch (expected: Exception) {
            collection.getLoggerService().info(expected, "Error removing Store (${collection.getKeyStringIdentifier(key)}) from MongoDB Layer")
        }
        return@withContext false
    }

    override suspend fun <K : Any, X : Store<X, K>> removeAll(collection: Collection<K, X>): Long = withContext(Dispatchers.IO) {
        try {
            // Delete All Documents in Mongo
            return@withContext getMongoCollection(collection).deleteMany(Filters.empty()).deletedCount
        } catch (ex: MongoException) {
            collection.getLoggerService().info(ex, "MongoDB error removing all Stores in Collection (${collection.name}) from MongoDB Layer")
        } catch (expected: Exception) {
            collection.getLoggerService().info(expected, "Error removing all Stores in Collection (${collection.name}) from MongoDB Layer")
        }
        return@withContext 0
    }

    override suspend fun <K : Any, X : Store<X, K>> getAll(collection: Collection<K, X>): Flow<X> {
        return getMongoCollection(collection).find().map { store: X ->
            collection.cacheIndexes(store, true)
            store
        }
    }

    override suspend fun <K : Any, X : Store<X, K>> getKeys(collection: Collection<K, X>): Flow<K> {
        val mongoCollection = getMongoCollection(collection).withDocumentClass<Document>()
        val idField = getSerialName(Store<X, K>::id)
        return mongoCollection.find().projection(Projections.include(idField)).mapNotNull { doc: Document ->
            // We know where the id is located, and we can fetch it, there is no need to deserialize the entire object
            val idString = doc.getString(idField) ?: return@mapNotNull null
            collection.keyFromString(idString)
        }
    }

    override fun canWrite(): Boolean {
        // just check that MongoDB is connected
        return mongoConnected
    }

    override fun <K : Any, X : Store<X, K>> onRegisteredCollection(collection: Collection<K, X>?) {
        // do nothing -> MongoDB handles it
    }

    // ------------------------------------------------- //
    //                MongoDB Connection                 //
    // ------------------------------------------------- //
    private suspend fun connectMongo(): Boolean {
        // Can only have one MongoClient instance
        var client = this.mongoClient
        Preconditions.checkState(client == null, "[MongoStorage] MongoClient instance already exists!")

        try {
            val monitor = MongoMonitor(this)
            val settingsBuilder = MongoClientSettings.builder()
                .uuidRepresentation(UuidRepresentation.STANDARD)
                .applyToClusterSettings { builder: ClusterSettings.Builder -> builder.addClusterListener(monitor) }
                .applyToServerSettings { builder: ServerSettings.Builder -> builder.addServerMonitorListener(monitor) }

            // Using connection URI
            settingsBuilder.applyConnectionString(ConnectionString(MongoConfig.get().uri))
            client = MongoClient.create(settingsBuilder.build())

            // Verify this client connection is valid
            try {
                DataStoreSource.get().logger.info("CONNECTING TO MONGODB (30 second timeout)...")
                val databaseNames = client.listDatabaseNames().toList()
                DataStoreSource.get().logger.info("Connection to MongoDB Succeeded! Databases:")
                DataStoreSource.get().logger.info(databaseNames.joinToString(", ", prefix = "[", postfix = "]"))

            } catch (timeout: MongoTimeoutException) {
                DataStoreFileLogger.warn("Connection to MongoDB Timed Out!", timeout)
                return false
            } catch (t: Throwable) {
                DataStoreFileLogger.warn("Failed to connect to MongoDB!", t)
                return false
            }

            // the datastore will be setup in the service class
            // If MongoDB fails it will automatically attempt to reconnect until it connects
            return true
        } catch (ex: Exception) {
            // Generic exception catch... just in case.
            return false // Failed to connect.
        } finally {
            this.mongoClient = client
            this.mongoConnected = client != null
        }
    }

    @Suppress("SameReturnValue")
    private fun disconnectMongo(): Boolean {
        val client = this.mongoClient ?: return true
        client.close()
        this.mongoClient = null
        this.mongoConnected = false
        return true
    }


    // ------------------------------------------------- //
    //             MongoCollection Management            //
    // ------------------------------------------------- //
    // Map<DatabaseName, MongoDatabase>
    private val dbMap: MutableMap<String, MongoDatabase> = ConcurrentHashMap()
    // Map<DatabaseName.CollectionName, MongoCollection<Document>>
    private val collMap: MutableMap<String, MongoCollection<*>> = ConcurrentHashMap()

    @Suppress("UNCHECKED_CAST")
    private fun <K : Any, X : Store<X, K>> getMongoCollection(collection: Collection<K, X>): MongoCollection<X> {
        val client = this.mongoClient ?: throw IllegalStateException("MongoClient is not initialized!")
        val collKey = collection.databaseName + "." + collection.name

        collMap[collKey]?.let {
            return it as MongoCollection<X>
        }

        val database = dbMap.computeIfAbsent(collection.databaseName) { databaseName: String -> client.getDatabase(databaseName) }
        val mongoColl = database.getCollection(collection.name, collection.storeClass)
        collMap[collKey] = mongoColl

        return mongoColl
    }


    override val isDebug: Boolean
        // ------------------------------------------------- //
        get() = DataStoreSource.isDebug()

    override val plugin: Plugin
        get() = DataStoreSource.get()

    override val loggerName: String
        get() = "MongoStorage"


    // ------------------------------------------------- //
    //                     Indexing                      //
    // ------------------------------------------------- //
    override suspend fun <K : Any, X : Store<X, K>, T> registerIndex(collection: StoreCollection<K, X>, index: IndexedField<X, T>) = withContext(Dispatchers.IO) {
        getMongoCollection(collection).createIndex(
            Document(index.name, 1),
            IndexOptions().unique(true)
        )
        Unit
    }

    override suspend fun <K : Any, X : Store<X, K>> cacheIndexes(collection: StoreCollection<K, X>, store: X, updateFile: Boolean) = withContext(Dispatchers.IO) {
        // do nothing -> MongoDB handles this
    }

    override suspend fun <K : Any, X : Store<X, K>> saveIndexCache(collection: StoreCollection<K, X>) = withContext(Dispatchers.IO) {
        // do nothing -> MongoDB handles this
    }

    override suspend fun <K : Any, X : Store<X, K>, T> getStoreByIndex(
        collection: StoreCollection<K, X>,
        index: IndexedField<X, T>,
        value: T
    ): X? = withContext(Dispatchers.IO) {
        // Filter by this index name (field name) and value (all indexes are unique)
        val store: X = getMongoCollection(collection)
            .find(
                Filters.eq(index.name, value)
            )
            .firstOrNull()
            ?: return@withContext null

        // Ensure index value equality
        if (!index.equals(index.getValue(store), value)) {
            return@withContext null
        }
        return@withContext store
    }

    override suspend fun <K : Any, X : Store<X, K>> invalidateIndexes(collection: StoreCollection<K, X>, key: K, updateFile: Boolean) = withContext(Dispatchers.IO) {
        // do nothing -> MongoDB handles this
    }

    fun setMongoPingNS(descriptions: List<ServerDescription>) {
        if (!this.mongoConnected) {
            return
        }
        descriptions.forEach { server: ServerDescription ->
            serverPingMap[server.address] = server.roundTripTimeNanos
        }
        this.recalculatePing()
    }

    @Transient
    private var pingNotifierTask: BukkitTask? = null
    private fun recalculatePing() {
        if (!this.mongoConnected) {
            return
        }
        var pingSumNS: Long = 0
        for (ping in serverPingMap.values) {
            pingSumNS += ping
        }
        this.averagePingNanos = pingSumNS / serverPingMap.size

        // Buffer the console log so any subsequent pings that come in within 1 second will be ignored
        // This is because we might be connected to a cluster, whose heartbeat pings will call this method individually
        if (pingNotifierTask == null) {
            pingNotifierTask = Bukkit.getScheduler().runTaskLater(DataStoreSource.get(), {
                pingNotifierTask = null
                this.debug("MongoDB Ping: " + ((this.averagePingNanos / 1000000L)) + "ms")
            }, 20L)
        }
    }
}
