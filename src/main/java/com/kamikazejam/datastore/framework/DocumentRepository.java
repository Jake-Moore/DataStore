package com.kamikazejam.datastore.framework;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.common.base.Preconditions;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.UpdateResult;

import lombok.Getter;

@SuppressWarnings({"unused", "FieldCanBeLocal"})
public class DocumentRepository<T extends BaseDocument<T>> {
    public static boolean BREAK = false; // TODO REMOVE

    private static final int MAX_RETRIES = 3;
    private final MongoCollection<Document> collection;
    private final MongoClient mongoClient;
    private final Class<T> entityClass;
    @Getter
    private final DocumentCache<T> cache;

    public DocumentRepository(MongoClient mongoClient, String database, String collectionName, Class<T> entityClass) {
        this.mongoClient = mongoClient;
        this.entityClass = entityClass;
        MongoDatabase db = mongoClient.getDatabase(database);
        this.collection = db.getCollection(collectionName);
        this.cache = new DocumentCache<>();
    }

    /**
     * Creates a new entity with the given initializer
     * Returns a read-only version of the created entity
     */
    @NotNull
    public T create(@NotNull Consumer<T> initializer) {
        Preconditions.checkNotNull(initializer, "Initializer cannot be null");

        try {
            // Create a new instance in modifiable state
            T entity = entityClass.getDeclaredConstructor().newInstance();
            entity.initialize();
            // Modify the Entity with our defaults
            entity.setReadOnly(false);
            entity.version.set(0L);
            
            // Initialize the entity
            initializer.accept(entity);
            
            // Save to database
            try (ClientSession session = mongoClient.startSession()) {
                session.startTransaction();
                boolean committed = false;
                T readOnlyEntity;
                
                try {
                    Document doc = JacksonUtil.serializeToDocument(entity);
                    collection.insertOne(session, doc);
                    session.commitTransaction();
                    committed = true;
                    
                    // Convert to read-only and cache
                    readOnlyEntity = entity.setReadOnly();
                    cache.put(entity.id.get(), readOnlyEntity);
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
    @NotNull
    public Optional<T> read(@NotNull String id) {
        Preconditions.checkNotNull(id, "ID cannot be null");

        T entity = findById(id);
        if (entity == null) {
            return Optional.empty();
        }
        return Optional.of(entity.setReadOnly());
    }

    /**
     * Modifies an entity in a controlled environment where modifications are allowed
     * Returns the updated read-only entity
     * @throws NoSuchElementException if the entity (by this id) is not found
     */
    @NotNull
    public T update(@NotNull String id, @NotNull Consumer<T> updateFunction) throws NoSuchElementException {
        Preconditions.checkNotNull(id, "ID cannot be null");
        Preconditions.checkNotNull(updateFunction, "Update function cannot be null");

        T originalEntity = findById(id);
        if (originalEntity == null) {
            throw new NoSuchElementException("Entity not found: " + id);
        }

        // Create a single base copy that we'll clone for each attempt
        T baseCopy = originalEntity.deepCopy();
        int attempts = 0;
        while (attempts < MAX_RETRIES) {
            // Clone the base copy for this attempt
            T workingCopy = baseCopy.deepCopy().setModifiable();
            long currentVersion = workingCopy.version.get();
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
                    Document doc = JacksonUtil.serializeToDocument(workingCopy);
                    UpdateResult result = collection.replaceOne(
                        session,
                        Filters.and(
                            Filters.eq("_id", id),
                            Filters.eq("version", currentVersion)
                        ),
                        doc
                    );
                    
                    if (result.getModifiedCount() == 0) {
                        // If update failed, fetch current version and update our base copy
                        Document currentDoc = collection.find(session).filter(Filters.eq("_id", id)).first();
                        if (currentDoc == null) {
                            throw new RuntimeException("Entity not found");
                        }
                        baseCopy = JacksonUtil.deserializeFromDocument(this.entityClass, currentDoc);
                        attempts++;
                        continue;
                    }
                    
                    // Success - update cache with new read-only version
                    finalEntity = workingCopy.setReadOnly();
                    session.commitTransaction();
                    committed = true;
                    
                    // Update the original entity with the new values and return it
                    originalEntity.setModifiable();
                    originalEntity.copyFieldsFrom(finalEntity);
                    cache.put(id, originalEntity);
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

    /**
     * Deletes an entity by ID
     * @return true if the entity was deleted, false if it was not found
     */
    public boolean delete(@NotNull String id) {
        Preconditions.checkNotNull(id, "ID cannot be null");
        
        // Invalidate cache first
        cache.invalidate(id);
        
        // Delete from database and return whether anything was deleted
        return collection.deleteOne(Filters.eq("_id", id)).getDeletedCount() > 0;
    }

    @Nullable
    private T findById(String id) {
        Optional<T> cached = cache.get(id);
        if (cached.isPresent()) {
            System.out.println("!!!! Cache hit for " + id);
            T cachedEntity = cached.get();
            cachedEntity.initialize();
            return cachedEntity;
        }

        Document doc = collection.find().filter(Filters.eq("_id", id)).first();
        if (doc != null) {
            System.out.println("!!!! Cache miss for " + id + " - fetched from DB");
            T entity = JacksonUtil.deserializeFromDocument(this.entityClass, doc);
            cache.put(id, entity.setReadOnly());
            return entity;
        }
        return null;
    }

    public static class DocumentCache<T extends BaseDocument<T>> {
        private final ConcurrentHashMap<String, CachedEntity<T>> cache = new ConcurrentHashMap<>();

        public void put(@NotNull String id, @NotNull T entity) {
            Preconditions.checkNotNull(id, "ID cannot be null");
            Preconditions.checkNotNull(entity, "Entity cannot be null");
            cache.put(id, new CachedEntity<>(entity));
        }

        @NotNull
        public Optional<T> get(@NotNull String id) {
            Preconditions.checkNotNull(id, "ID cannot be null");
            @Nullable CachedEntity<T> cached = cache.get(id);
            if (cached != null && cached.isStale()) {
                cache.remove(id);
                return Optional.empty();
            }
            return Optional.ofNullable(cached).map(CachedEntity::getEntity);
        }

        public void invalidate(@NotNull String id) {
            Preconditions.checkNotNull(id, "ID cannot be null");
            cache.remove(id);
        }

        public void clear() {
            cache.clear();
        }
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