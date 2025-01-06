package com.kamikazejam.datastore.example.framework;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.bson.UuidRepresentation;
import org.bson.types.ObjectId;
import org.mongojack.JacksonMongoCollection;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.UpdateResult;

import static com.kamikazejam.datastore.example.Example.BREAK;

@SuppressWarnings({"unused", "FieldCanBeLocal"})
public class MongoRepository<T extends BaseDocument<T>> {
    private static final int MAX_RETRIES = 3;
    private final JacksonMongoCollection<T> collection;
    private final MongoClient mongoClient;
    private final Class<T> entityClass;
    private final ConcurrentHashMap<ObjectId, CachedEntity<T>> cache;
    
    public MongoRepository(MongoClient mongoClient, String database, String collectionName, Class<T> entityClass) {
        this.mongoClient = mongoClient;
        this.entityClass = entityClass;
        MongoDatabase db = mongoClient.getDatabase(database);
        MongoCollection<T> mongoCollection = db.getCollection(collectionName, entityClass);
        this.collection = JacksonMongoCollection.builder()
            .withObjectMapper(JacksonUtil.getObjectMapper())
            .build(mongoCollection, entityClass, UuidRepresentation.STANDARD);
        this.cache = new ConcurrentHashMap<>();
    }
    
    /**
     * Creates a new entity with the given initializer
     * Returns a read-only version of the created entity
     */
    public T create(Consumer<T> initializer) {
        try {
            // Create a new instance in modifiable state
            T entity = entityClass.getDeclaredConstructor().newInstance();
            entity.initialize(); // Ensure all fields have parent reference
            // Modify the Entity with our defaults
            entity.setReadOnly(false);
            entity.id.set(new ObjectId());
            entity.version.set(0L);
            
            // Initialize the entity
            initializer.accept(entity);
            
            // Save to database
            try (ClientSession session = mongoClient.startSession()) {
                session.startTransaction();
                boolean committed = false;
                T readOnlyEntity;
                
                try {
                    collection.insertOne(session, entity);
                    session.commitTransaction();
                    committed = true;
                    
                    // Convert to read-only and cache
                    readOnlyEntity = entity.setReadOnly();
                    cache.put(entity.id.get(), new CachedEntity<>(readOnlyEntity));
                } finally {
                    if (!committed) {
                        session.abortTransaction();
                    }
                }
                
                return readOnlyEntity;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create entity", e);
        }
    }
    
    /**
     * Gets a read-only version of the entity
     */
    public T get(ObjectId id) {
        T entity = findById(id);
        if (entity == null) {
            return null;
        }
        return entity.setReadOnly();
    }
    
    /**
     * Modifies an entity in a controlled environment where modifications are allowed
     * Returns the updated read-only entity
     */
    public T modify(ObjectId id, Consumer<T> updateFunction) {
        T originalEntity = findById(id);
        if (originalEntity == null) {
            throw new RuntimeException("Entity not found: " + id);
        }

        // Create a single base copy that we'll clone for each attempt
        T baseCopy = originalEntity.deepCopy();
        int attempts = 0;
        while (attempts < MAX_RETRIES) {
            // Clone the base copy for this attempt
            T workingCopy = baseCopy.deepCopy().setModifiable();
            long currentVersion = workingCopy.version.get(); // fetch before the updateFunction
            if (BREAK && attempts == 0) {
                currentVersion++; // force the update to fail the first time
            }

            // Apply updates to the copy
            updateFunction.accept(workingCopy);
            // Increment version (Optimistic Versioning)
            workingCopy.version.set(currentVersion + 1);

            try (ClientSession session = mongoClient.startSession()) {
                session.startTransaction();
                boolean committed = false;
                T finalEntity;
                
                try {
                    UpdateResult result = collection.replaceOne(
                        session,
                        Filters.and(
                            Filters.eq("_id.value", id),
                            Filters.eq("version.value", currentVersion)
                        ),
                        workingCopy
                    );
                    
                    if (result.getModifiedCount() == 0) {
                        System.out.println("!!!! Update failed for " + id + " - retrying");
                        // If update failed, fetch current version and update our base copy
                        T tempEntity = collection.find(session).filter(Filters.eq("_id.value", id)).first();
                        if (tempEntity == null) {
                            throw new RuntimeException("Entity not found");
                        }
                        tempEntity.initialize();
                        baseCopy = tempEntity.deepCopy(); // Update our base copy for next attempt
                        attempts++;
                        continue;
                    }
                    
                    // Success - update cache with new read-only version
                    finalEntity = workingCopy.setReadOnly();
                    session.commitTransaction();
                    committed = true;
                    
                    // Update the original entity with the new values and return it
                    // Make sure this originalEntity is placed in cache, not the workingCopy
                    originalEntity.setModifiable();
                    originalEntity.copyFieldsFrom(finalEntity);
                    cache.put(id, new CachedEntity<>(originalEntity));
                    return originalEntity.setReadOnly();
                    
                } finally {
                    if (!committed) {
                        session.abortTransaction();
                    }
                }
            } catch (Exception e) {
                attempts++;
                if (attempts >= MAX_RETRIES) {
                    throw new RuntimeException("Failed to update after " + MAX_RETRIES + " attempts", e);
                }
            }
        }
        throw new RuntimeException("Failed to update after " + MAX_RETRIES + " attempts");
    }

    private T findById(ObjectId id) {
        CachedEntity<T> cached = cache.get(id);
        if (cached != null && !cached.isStale()) {
            System.out.println("!!!! Cache hit for " + id);
            T cachedEntity = cached.getEntity();
            cachedEntity.initialize();
            return cachedEntity;
        }

        T entity = collection.find().filter(Filters.eq("_id.value", id)).first();
        if (entity != null) {
            System.out.println("!!!! Cache miss for " + id + " - fetched from DB");
            entity.initialize(); // Ensure entity is initialized after deserialization
            cache.put(id, new CachedEntity<>(entity.setReadOnly()));
            return entity;
        }
        return null;
    }
    
    public void invalidateCache(ObjectId id) {
        cache.remove(id);
    }
    
    public void clearCache() {
        cache.clear();
    }
    
    private static class CachedEntity<T extends BaseDocument<T>> {
        private static final long TTL_MS = 30_000; // 30 seconds TTL
        private final T entity;
        private final long expiresAt;
        
        CachedEntity(T entity) {
            this.entity = entity;
            this.expiresAt = System.currentTimeMillis() + TTL_MS;
        }
        
        T getEntity() {
            return entity;
        }
        
        boolean isStale() {
            return System.currentTimeMillis() > expiresAt;
        }
    }
} 