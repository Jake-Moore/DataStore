package com.kamikazejam.datastore.connections.storage.mongo;

import com.google.common.base.Preconditions;
import com.kamikazejam.datastore.DataStoreSource;
import com.kamikazejam.datastore.base.Cache;
import com.kamikazejam.datastore.base.Store;
import com.kamikazejam.datastore.base.StoreCache;
import com.kamikazejam.datastore.base.index.IndexedField;
import com.kamikazejam.datastore.connections.config.MongoConfig;
import com.kamikazejam.datastore.connections.monitor.MongoMonitor;
import com.kamikazejam.datastore.connections.storage.StorageService;
import com.kamikazejam.datastore.connections.storage.iterator.TransformingIterator;
import com.kamikazejam.datastore.util.DataStoreFileLogger;
import com.kamikazejam.datastore.util.JacksonUtil;
import com.mongodb.*;
import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Projections;
import com.mongodb.connection.ServerDescription;
import lombok.Getter;
import lombok.Setter;
import org.bson.Document;
import org.bson.UuidRepresentation;
import org.bson.conversions.Bson;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.StreamSupport;

import static com.kamikazejam.datastore.util.JacksonUtil.ID_FIELD;

@Getter
@SuppressWarnings({"unused"})
public class MongoStorage extends StorageService {
    private boolean running = false;
    @Setter
    private boolean mongoInitConnect = false;
    @Setter
    private boolean mongoConnected = false;
    private final Map<ServerAddress, Long> serverPingMap = new HashMap<>();
    private long mongoPingNS = 1_000_000; // Default to 1ms (is updated every cluster and heartbeat event)

    // MongoDB
    @Getter
    private MongoClient mongoClient = null;

    public MongoStorage() {
    }

    // ------------------------------------------------- //
    //                     Service                       //
    // ------------------------------------------------- //
    @Override
    public boolean start() {
        this.debug("Connecting to MongoDB");
        // Load Mapper on start-up
        JacksonUtil.getObjectMapper();

        boolean mongo = this.connectMongo();
        this.running = true;

        if (!mongo) {
            this.error("Failed to start MongoStorage, connection failed.");
            return false;
        }
        // Client was created, MongoMonitor should log more as the connection succeeds or fails
        return true;
    }

    @Override
    public boolean shutdown() {
        // If not running, warn and return true (we are already shutdown)
        if (!running) {
            this.warn("MongoStorage.shutdown() called while service is not running!");
            return true;
        }

        // Disconnect from MongoDB
        boolean mongo = this.disconnectMongo();
        this.running = false;

        if (!mongo) {
            this.error("Failed to shutdown MongoStorage, disconnect failed.");
            return false;
        }

        this.debug("Disconnected from MongoDB");
        return true;
    }


    // ------------------------------------------------- //
    //                StorageService                     //
    // ------------------------------------------------- //

    @Override
    public @NotNull <K, X extends Store<X, K>> Optional<X> get(Cache<K, X> cache, K key) {
        Preconditions.checkNotNull(key);
        try {
            MongoCollection<Document> coll = this.getMongoCollection(cache);
            Document doc = coll.find().filter(Filters.eq(ID_FIELD, cache.keyToString(key))).first();
            if (doc == null) { return Optional.empty(); }

            Optional<X> o = Optional.of(JacksonUtil.deserializeFromDocument(cache.getStoreClass(), doc));
            // Cache Indexes since we are loading from database
            o.ifPresent(s -> cache.cacheIndexes(s, true));

            return o;
        } catch (MongoException ex) {
            cache.getLoggerService().info(ex, "MongoDB error getting Object from MongoDB Layer: " + key);
            return Optional.empty();
        } catch (Exception expected) {
            cache.getLoggerService().info(expected, "Error getting Object from MongoDB Layer: " + key);
            return Optional.empty();
        }
    }

    @Override
    public <K, X extends Store<X, K>> boolean save(Cache<K, X> cache, X store) {
        // Save to database with a transaction & only 1 attempt
        try (ClientSession session = mongoClient.startSession()) {
            session.startTransaction();
            boolean committed = false;

            try {
                Document doc = JacksonUtil.serializeToDocument(store);
                getMongoCollection(cache).insertOne(session, doc);
                session.commitTransaction();
                committed = true;

                // Convert to read-only and cache
                store.setReadOnly(true);
                cache.cache(store);
            } finally {
                if (!committed) {
                    session.abortTransaction();
                }
            }
        }
        return true;
    }

    // This method needs no additional concurrency safeguards, because of the transactional nature of the update
    //  and the compare-and-swap design of the mongo query & its filters.
    // If two threads enter this method for the same object, only one will succeed in updating the object.
    //  and the other will have its query fail and will automatically retry.
    // (MongoDB provides the document-level locking already)
    @Override
    public <K, X extends Store<X, K>> boolean update(Cache<K, X> cache, X originalStore, @NotNull Consumer<X> updateFunction) {
        // Create a single base copy that we'll clone for each attempt
        final X baseCopyFinal = JacksonUtil.deepCopy(originalStore);

        try {
            final MongoCollection<Document> collection = this.getMongoCollection(cache)
                    .withWriteConcern(WriteConcern.MAJORITY);  // Ensure replication

            return MongoTransactionHelper.executeUpdate(mongoClient, collection, cache, originalStore, updateFunction);
        } catch (Exception e) {
            DataStoreFileLogger.warn("Failed to update Store in MongoDB Layer after all retries: " + originalStore.getId(), e);
            return false;
        }
    }

    @Override
    public <K, X extends Store<X, K>> boolean has(Cache<K, X> cache, K key) {
        Preconditions.checkNotNull(key);
        try {
            Bson query = Filters.eq(ID_FIELD, cache.keyToString(key));
            return getMongoCollection(cache).countDocuments(query) > 0;
        } catch (MongoException ex) {
            cache.getLoggerService().info(ex, "MongoDB error check if Store exists in MongoDB Layer: " + key);
            return false;
        } catch (Exception expected) {
            cache.getLoggerService().info(expected, "Error checking if Store exists in MongoDB Layer: " + key);
            return false;
        }
    }

    @Override
    public <K, X extends Store<X, K>> long size(Cache<K, X> cache) {
        return getMongoCollection(cache).countDocuments();
    }

    @Override
    public <K, X extends Store<X, K>> boolean remove(Cache<K, X> cache, K key) {
        Preconditions.checkNotNull(key);
        try {
            Bson query = Filters.eq(ID_FIELD, cache.keyToString(key));
            return getMongoCollection(cache).deleteMany(query).getDeletedCount() > 0;
        } catch (MongoException ex) {
            cache.getLoggerService().info(ex, "MongoDB error removing Store from MongoDB Layer: " + key);
        } catch (Exception expected) {
            cache.getLoggerService().info(expected, "Error removing Store from MongoDB Layer: " + key);
        }
        return false;
    }

    @Override
    public <K, X extends Store<X, K>> Iterable<X> getAll(Cache<K, X> cache) {
        return () -> new TransformingIterator<>(getMongoCollection(cache).find().iterator(), doc -> {
            X store = JacksonUtil.deserializeFromDocument(cache.getStoreClass(), doc);
            // Make sure to cache indexes when a store is loaded from the database
            cache.cacheIndexes(store, true);
            return store;
        });
    }

    @Override
    public <K, X extends Store<X, K>> Iterable<K> getKeys(Cache<K, X> cache) {
        // Fetch all documents, but use Projection to only retrieve the ID field
        MongoCursor<Document> docs = getMongoCollection(cache).find().projection(Projections.include(ID_FIELD)).iterator();
        // We know where the id is located, and we can fetch it as a string, there is no need to deserialize the entire object
        return () -> new TransformingIterator<>(docs, doc -> cache.keyFromString(doc.getString(ID_FIELD)));
    }

    @Override
    public boolean canCache() {
        // just check that MongoDB is connected
        return mongoConnected;
    }

    @Override
    public <K, X extends Store<X, K>> void onRegisteredCache(Cache<K, X> cache) {
        // do nothing -> MongoDB handles it
    }

    // ------------------------------------------------- //
    //                MongoDB Connection                 //
    // ------------------------------------------------- //
    public boolean connectMongo() {
        // Can only have one MongoClient instance
        Preconditions.checkState(this.mongoClient == null, "[MongoStorage] MongoClient instance already exists!");

        try {
            final MongoMonitor monitor = new MongoMonitor(this);
            MongoClientSettings.Builder settingsBuilder = MongoClientSettings.builder()
                    .uuidRepresentation(UuidRepresentation.STANDARD)
                    .applyToClusterSettings(builder -> builder.addClusterListener(monitor))
                    .applyToServerSettings(builder -> builder.addServerMonitorListener(monitor));

            // Using connection URI
            ConnectionString connectionString = new ConnectionString(MongoConfig.get().getUri());
            settingsBuilder.applyConnectionString(connectionString);
            this.mongoClient = MongoClients.create(settingsBuilder.build());

            // Verify this client connection is valid
            try {
                DataStoreSource.get().getLogger().info("CONNECTING TO MONGODB (30 second timeout)...");
                List<String> ignored = StreamSupport.stream(this.mongoClient.listDatabaseNames().spliterator(), false).toList();
            }catch (MongoTimeoutException timeout) {
                DataStoreFileLogger.warn("Connection to MongoDB Timed Out!", timeout);
                return false;
            }catch (Throwable t) {
                DataStoreFileLogger.warn("Failed to connect to MongoDB!", t);
                return false;
            }

            // the datastore will be setup in the service class
            // If MongoDB fails it will automatically attempt to reconnect until it connects
            return true;
        } catch (Exception ex) {
            // Generic exception catch... just in case.
            return false; // Failed to connect.
        }
    }

    private boolean disconnectMongo() {
        if (this.mongoClient != null) {
            this.mongoClient.close();
            this.mongoClient = null;
            this.mongoConnected = false;
        }
        return true;
    }


    // ------------------------------------------------- //
    //             MongoCollection Management            //
    // ------------------------------------------------- //
    private final Map<String, MongoDatabase> dbMap = new HashMap<>();               // Map<DatabaseName, MongoDatabase>
    private final Map<String, MongoCollection<Document>> collMap = new HashMap<>(); // Map<DatabaseName.CollectionName, MongoCollection<Document>>
    public <K, X extends Store<X, K>> @NotNull MongoCollection<Document> getMongoCollection(Cache<K, X> cache) {
        String collKey = cache.getDatabaseName() + "." + cache.getName();
        if (collMap.containsKey(collKey)) { return collMap.get(collKey); }

        MongoDatabase database = dbMap.computeIfAbsent(cache.getDatabaseName(), mongoClient::getDatabase);
        MongoCollection<Document> collection = database.getCollection(cache.getName());
        collMap.put(collKey, collection);

        return collection;
    }


    // ------------------------------------------------- //
    //                   LoggerService                   //
    // ------------------------------------------------- //
    @Override
    public boolean isDebug() {
        return DataStoreSource.isDebug();
    }

    @Override
    public Plugin getPlugin() {
        return DataStoreSource.get();
    }

    @Override
    public String getLoggerName() {
        return "MongoStorage";
    }



    // ------------------------------------------------- //
    //                     Indexing                      //
    // ------------------------------------------------- //

    @Override
    public <K, X extends Store<X, K>, T> void registerIndex(@NotNull StoreCache<K, X> cache, IndexedField<X, T> index) {
        getMongoCollection(cache).createIndex(
                new Document(index.getName(), 1),
                new IndexOptions().unique(true)
        );
    }
    @Override
    public <K, X extends Store<X, K>> void cacheIndexes(@NotNull StoreCache<K, X> cache, @NotNull X store, boolean updateFile) {
        // do nothing -> MongoDB handles this
    }
    @Override
    public <K, X extends Store<X, K>> void saveIndexCache(@NotNull StoreCache<K, X> cache) {
        // do nothing -> MongoDB handles this
    }
    @Override
    public <K, X extends Store<X, K>, T> @Nullable K getStoreIdByIndex(@NotNull StoreCache<K, X> cache, IndexedField<X, T> index, T value) {
        // Fetch an object with the given index value, projecting only the ID and the index field
        Bson query = Filters.eq(index.getName(), value);
        @Nullable Document doc = getMongoCollection(cache).find(query).projection(Projections.include(ID_FIELD, index.getName())).first();
        if (doc == null) { return null; }
        X store = JacksonUtil.deserializeFromDocument(cache.getStoreClass(), doc);
        // Ensure index value equality
        if (!index.equals(index.getValue(store), value)) {
            return null;
        }
        return store.getId();
    }

    @Override
    public <K, X extends Store<X, K>> void invalidateIndexes(@NotNull StoreCache<K, X> cache, @NotNull K key, boolean updateFile) {
        // do nothing -> MongoDB handles this
    }

    @Override
    public long getPingNano() {
        Preconditions.checkNotNull(mongoClient);
        Preconditions.checkState(mongoConnected);

        try {
            // Get the ping to MongoDB
            long nanos = System.nanoTime();
            mongoClient.listDatabaseNames().first();
            return System.nanoTime() - nanos;
        } catch (Exception ex) {
            return -1;
        }
    }

    @Override
    public long getAveragePingNanos() {
        return this.mongoPingNS;
    }

    public void setMongoPingNS(List<ServerDescription> descriptions) {
        if (!this.mongoConnected) { return; }
        descriptions.forEach(server -> this.serverPingMap.put(server.getAddress(), server.getRoundTripTimeNanos()));
        this.recalculatePing();
    }

    private void recalculatePing() {
        if (!this.mongoConnected) { return; }
        long pingSumNS = 0;
        for (long ping : this.serverPingMap.values()) {
            pingSumNS += ping;
        }
        this.mongoPingNS = pingSumNS / this.serverPingMap.size();
        this.debug("MongoDB Ping: " + ((this.mongoPingNS / 1_000_000L)) + "ms");
    }
}
