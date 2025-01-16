package com.kamikazejam.datastore.connections.storage.mongo

import com.google.common.base.Preconditions
import com.kamikazejam.datastore.DataStoreSource
import com.kamikazejam.datastore.base.Cache
import com.kamikazejam.datastore.base.Store
import com.kamikazejam.datastore.base.StoreCache
import com.kamikazejam.datastore.base.index.IndexedField
import com.kamikazejam.datastore.connections.config.MongoConfig
import com.kamikazejam.datastore.connections.monitor.MongoMonitor
import com.kamikazejam.datastore.connections.storage.StorageService
import com.kamikazejam.datastore.connections.storage.iterator.TransformingIterator
import com.kamikazejam.datastore.util.DataStoreFileLogger
import com.kamikazejam.datastore.util.JacksonUtil
import com.mongodb.*
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Projections
import com.mongodb.connection.ClusterSettings
import com.mongodb.connection.ServerDescription
import com.mongodb.connection.ServerSettings
import org.bson.Document
import org.bson.UuidRepresentation
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask
import java.util.function.Consumer
import java.util.stream.StreamSupport

@Suppress("unused")
class MongoStorage : StorageService() {
    override var running = false

    var mongoInitConnect = false

    var mongoConnected = false
    private val serverPingMap: MutableMap<ServerAddress, Long> = HashMap()
    override var averagePingNanos: Long = 1000000 // Default to 1ms (is updated every cluster and heartbeat event)
        private set

    // MongoDB
    var mongoClient: MongoClient? = null

    // ------------------------------------------------- //
    //                     Service                       //
    // ------------------------------------------------- //
    override fun start(): Boolean {
        this.debug("Connecting to MongoDB")
        // Load Mapper on start-up
        JacksonUtil.objectMapper

        val mongo = this.connectMongo()
        this.running = true

        if (!mongo) {
            this.error("Failed to start MongoStorage, connection failed.")
            return false
        }
        // Client was created, MongoMonitor should log more as the connection succeeds or fails
        return true
    }

    override fun shutdown(): Boolean {
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
    override fun <K, X : Store<X, K>> get(cache: Cache<K, X>, key: K): X? {
        try {
            val coll = this.getMongoCollection(cache)
            val doc = coll.find().filter(Filters.eq(JacksonUtil.ID_FIELD, cache.keyToString(key))).first() ?: return null

            val o: X = JacksonUtil.deserializeFromDocument(cache.storeClass, doc)
            // Cache Indexes since we are loading from database
            cache.cacheIndexes(o, true)

            return o
        } catch (ex: MongoException) {
            cache.getLoggerService().info(ex, "MongoDB error getting Object from MongoDB Layer: $key")
            return null
        } catch (expected: Exception) {
            cache.getLoggerService().info(expected, "Error getting Object from MongoDB Layer: $key")
            return null
        }
    }

    override fun <K, X : Store<X, K>> save(cache: Cache<K, X>, store: X): Boolean {
        // Save to database with a transaction & only 1 attempt
        mongoClient!!.startSession().use { session ->
            session.startTransaction()
            var committed = false
            try {
                val doc = JacksonUtil.serializeToDocument(store)
                getMongoCollection(cache).insertOne(session, doc)
                session.commitTransaction()
                committed = true

                // Convert to read-only and cache
                store.readOnly = true
                cache.cache(store)
            } finally {
                if (!committed) {
                    session.abortTransaction()
                }
            }
        }
        return true
    }

    // This method needs no additional concurrency safeguards, because of the transactional nature of the update
    //  and the compare-and-swap design of the mongo query & its filters.
    // If two threads enter this method for the same object, only one will succeed in updating the object.
    //  and the other will have its query fail and will automatically retry.
    // (MongoDB provides the document-level locking already)
    override fun <K, X : Store<X, K>> updateSync(
        cache: Cache<K, X>,
        store: X,
        updateFunction: Consumer<X>
    ): Boolean {
        try {
            val collection = getMongoCollection(cache)
                .withWriteConcern(WriteConcern.MAJORITY) // Ensure replication

            return MongoTransactionHelper.executeUpdate(mongoClient!!, collection, cache, store, updateFunction)
        } catch (e: Exception) {
            DataStoreFileLogger.warn(
                "Failed to update Store in MongoDB Layer after all retries: " + store.id,
                e
            )
            return false
        }
    }

    override fun <K, X : Store<X, K>> has(cache: Cache<K, X>, key: K): Boolean {
        try {
            val query = Filters.eq(JacksonUtil.ID_FIELD, cache.keyToString(key))
            return getMongoCollection(cache).countDocuments(query) > 0
        } catch (ex: MongoException) {
            cache.getLoggerService().info(ex, "MongoDB error check if Store exists in MongoDB Layer: $key")
            return false
        } catch (expected: Exception) {
            cache.getLoggerService().info(expected, "Error checking if Store exists in MongoDB Layer: $key")
            return false
        }
    }

    override fun <K, X : Store<X, K>> size(cache: Cache<K, X>): Long {
        return getMongoCollection(cache).countDocuments()
    }

    override fun <K, X : Store<X, K>> remove(cache: Cache<K, X>, key: K): Boolean {
        try {
            val query = Filters.eq(JacksonUtil.ID_FIELD, cache.keyToString(key))
            return getMongoCollection(cache).deleteMany(query).deletedCount > 0
        } catch (ex: MongoException) {
            cache.getLoggerService().info(ex, "MongoDB error removing Store from MongoDB Layer: $key")
        } catch (expected: Exception) {
            cache.getLoggerService().info(expected, "Error removing Store from MongoDB Layer: $key")
        }
        return false
    }

    override fun <K, X : Store<X, K>> getAll(cache: Cache<K, X>): Iterable<X> {
        return Iterable {
            TransformingIterator<Document, X>(getMongoCollection(cache).find().iterator()) { doc: Document ->
                val store: X = JacksonUtil.deserializeFromDocument(cache.storeClass, doc)
                // Make sure to cache indexes when a store is loaded from the database
                cache.cacheIndexes(store, true)
                store
            }
        }
    }

    override fun <K, X : Store<X, K>> getKeys(cache: Cache<K, X>): Iterable<K> {
        // Fetch all documents, but use Projection to only retrieve the ID field
        val docs = getMongoCollection(cache).find().projection(Projections.include(JacksonUtil.ID_FIELD)).iterator()
        // We know where the id is located, and we can fetch it as a string, there is no need to deserialize the entire object
        return Iterable {
            TransformingIterator(docs) {
                doc: Document -> cache.keyFromString(doc.getString(JacksonUtil.ID_FIELD))
            }
        }
    }

    override fun canCache(): Boolean {
        // just check that MongoDB is connected
        return mongoConnected
    }

    override fun <K, X : Store<X, K>> onRegisteredCache(cache: Cache<K, X>?) {
        // do nothing -> MongoDB handles it
    }

    // ------------------------------------------------- //
    //                MongoDB Connection                 //
    // ------------------------------------------------- //
    private fun connectMongo(): Boolean {
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
            val connectionString = ConnectionString(MongoConfig.get().uri)
            settingsBuilder.applyConnectionString(connectionString)
            client = MongoClients.create(settingsBuilder.build())

            // Verify this client connection is valid
            try {
                DataStoreSource.get().logger.info("CONNECTING TO MONGODB (30 second timeout)...")
                StreamSupport.stream(client.listDatabaseNames().spliterator(), false).toList()
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
        }
    }

    private fun disconnectMongo(): Boolean {
        if (this.mongoClient != null) {
            mongoClient!!.close()
            this.mongoClient = null
            this.mongoConnected = false
        }
        return true
    }


    // ------------------------------------------------- //
    //             MongoCollection Management            //
    // ------------------------------------------------- //
    private val dbMap: MutableMap<String, MongoDatabase> = HashMap() // Map<DatabaseName, MongoDatabase>
    private val collMap: MutableMap<String, MongoCollection<Document>> =
        HashMap() // Map<DatabaseName.CollectionName, MongoCollection<Document>>

    private fun <K, X : Store<X, K>> getMongoCollection(cache: Cache<K, X>): MongoCollection<Document> {
        val collKey = cache.databaseName + "." + cache.name
        if (collMap.containsKey(collKey)) {
            return collMap[collKey]!!
        }

        val database = dbMap.computeIfAbsent(cache.databaseName) { databaseName: String -> mongoClient!!.getDatabase(databaseName) }
        val collection = database.getCollection(cache.name)
        collMap[collKey] = collection

        return collection
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
    override fun <K, X : Store<X, K>, T> registerIndex(cache: StoreCache<K, X>, index: IndexedField<X, T>) {
        getMongoCollection(cache).createIndex(
            Document(index.name, 1),
            IndexOptions().unique(true)
        )
    }

    override fun <K, X : Store<X, K>> cacheIndexes(cache: StoreCache<K, X>, store: X, updateFile: Boolean) {
        // do nothing -> MongoDB handles this
    }

    override fun <K, X : Store<X, K>> saveIndexCache(cache: StoreCache<K, X>) {
        // do nothing -> MongoDB handles this
    }

    override fun <K, X : Store<X, K>, T> getStoreIdByIndex(
        cache: StoreCache<K, X>,
        index: IndexedField<X, T>,
        value: T
    ): K? {
        // Fetch an object with the given index value, projecting only the ID and the index field
        val query = Filters.eq(index.name, value)
        val doc = getMongoCollection(cache).find(query)
            .projection(Projections.include(JacksonUtil.ID_FIELD, index.name))
            .first()
            ?: return null
        val store: X = JacksonUtil.deserializeFromDocument(cache.storeClass, doc)
        // Ensure index value equality
        if (!index.equals(index.getValue(store), value)) {
            return null
        }
        return store.id
    }

    override fun <K, X : Store<X, K>> invalidateIndexes(cache: StoreCache<K, X>, key: K, updateFile: Boolean) {
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
