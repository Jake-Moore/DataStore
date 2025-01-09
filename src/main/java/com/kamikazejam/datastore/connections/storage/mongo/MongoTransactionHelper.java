package com.kamikazejam.datastore.connections.storage.mongo;

import com.google.common.base.Preconditions;
import com.kamikazejam.datastore.DataStoreSource;
import com.kamikazejam.datastore.base.Cache;
import com.kamikazejam.datastore.base.Store;
import com.kamikazejam.datastore.connections.storage.exception.TransactionRetryLimitExceededException;
import com.kamikazejam.datastore.util.DataStoreFileLogger;
import com.kamikazejam.datastore.util.JacksonUtil;
import com.mongodb.MongoCommandException;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;

import java.util.Random;
import java.util.function.Consumer;

import static com.mongodb.client.model.Filters.eq;

@SuppressWarnings("UnusedReturnValue")
class MongoTransactionHelper {
    private static final Random RANDOM = new Random();
    private static final int DEFAULT_MAX_RETRIES = 15;
    private static final int BASE_BACKOFF_MS = 50;      // Start with 50ms delay
    private static final int LINEAR_BACKOFF_MS = 20;    // Increase 20ms per attempt
    private static final int WRITE_CONFLICT_ERROR = 112;

    /**
     * Execute a MongoDB document update with retries and version checking
     * @param mongoClient The MongoDB client
     * @param collection The MongoDB collection
     * @param cache The cache containing the document
     * @param originalStore The original store to update
     * @param updateFunction The function to apply updates
     * @return Whether the update was successful
     */
    protected static <K, X extends Store<X, K>> boolean executeUpdate(
            @NotNull MongoClient mongoClient,
            @NotNull MongoCollection<Document> collection,
            @NotNull Cache<K, X> cache,
            @NotNull X originalStore,
            @NotNull Consumer<X> updateFunction
    ) {
        Preconditions.checkNotNull(mongoClient, "MongoClient cannot be null");
        Preconditions.checkNotNull(collection, "Collection cannot be null");
        Preconditions.checkNotNull(cache, "Cache cannot be null");
        Preconditions.checkNotNull(originalStore, "Store cannot be null");
        Preconditions.checkNotNull(updateFunction, "Update function cannot be null");

        try {
            // Create working copy that will be updated on each attempt
            X baseCopy = JacksonUtil.deepCopy(originalStore);
            executeUpdateInternal(mongoClient, collection, cache, originalStore, baseCopy, updateFunction, 0);
            return true;
        }catch (TransactionRetryLimitExceededException e){
            DataStoreFileLogger.warn("Failed to execute MongoDB update in " + DEFAULT_MAX_RETRIES + " attempts");
            return false;
        }
    }

    // If no error is thrown, this method succeeded
    private static <K, X extends Store<X, K>> void executeUpdateInternal(
            @NotNull MongoClient mongoClient,
            @NotNull MongoCollection<Document> collection,
            @NotNull Cache<K, X> cache,
            @NotNull X originalStore,
            @NotNull X baseCopy,
            @NotNull Consumer<X> updateFunction,
            int currentAttempt
    ) throws TransactionRetryLimitExceededException {
        // Quit if we've run out of attempts
        if (currentAttempt >= DEFAULT_MAX_RETRIES) {
            throw new TransactionRetryLimitExceededException("Failed to execute update after " + DEFAULT_MAX_RETRIES + " attempts.");
        }

        // Apply exponential backoff if this isn't our first attempt
        if (currentAttempt > 0) {
            applyBackoff(currentAttempt);
        }

        try (ClientSession session = mongoClient.startSession()) {
            session.startTransaction();
            boolean committed = false;

            try {
                // Clone the base copy for this attempt
                X workingCopy = JacksonUtil.deepCopy(baseCopy);
                workingCopy.setReadOnly(false);

                // Fetch Version prior to updates
                long currentVersion = workingCopy.getVersion().get();

                // Apply updates to the copy
                updateFunction.accept(workingCopy);
                // Increment version (Optimistic Versioning)
                workingCopy.getVersion().set(currentVersion + 1);

                final String id = cache.keyToString(workingCopy.getId());
                Document doc = JacksonUtil.serializeToDocument(workingCopy);

                UpdateResult result = collection.replaceOne(
                        session,
                        Filters.and(
                                // These two filters act as a sort of compare-and-swap mechanic
                                //  inside of this mongo transaction, if these are not met then
                                //  the transaction will fail and we will need to retry.
                                eq("_id", id),
                                eq("version", currentVersion)
                        ),
                        doc
                );

                // If no documents were modified, then the compare-and-swap failed, we must retry
                if (result.getModifiedCount() == 0) {
                    DataStoreSource.getColorLogger().debug("Failed to update Store in MongoDB Layer (Could not find document with id: '" + id + "' and version: " + currentVersion + ")");

                    // If update failed, fetch current version
                    Document currentDoc = collection.find(session).filter(eq("_id", id)).first();
                    if (currentDoc == null) {
                        throw new RuntimeException("Entity not found");
                    }

                    // Update our working copy with latest version and retry
                    baseCopy = JacksonUtil.deserializeFromDocument(cache.getStoreClass(), currentDoc);
                    executeUpdateInternal(mongoClient, collection, cache, originalStore, baseCopy, updateFunction, currentAttempt + 1);
                    return;
                }

                // Success - update the cached store from our working copy
                workingCopy.setReadOnly(true);
                originalStore.setReadOnly(false);
                cache.updateStoreFromNewer(originalStore, workingCopy);
                cache.cache(originalStore);
                originalStore.setReadOnly(true);

                session.commitTransaction();
                committed = true;

            } catch (TransactionRetryLimitExceededException t) {
                // re-throw to bubble up
                throw t;
            } catch (MongoCommandException mE) {
                if (isWriteConflict(mE)) {
                    logWriteConflict(currentAttempt);
                    // For write conflicts, retry with same working copy
                    executeUpdateInternal(mongoClient, collection, cache, originalStore, baseCopy, updateFunction, currentAttempt + 1);
                    return;
                }
                throw mE;
            } catch (Exception e) {
                DataStoreFileLogger.warn("Failed to execute MongoDB update", e);
                executeUpdateInternal(mongoClient, collection, cache, originalStore, baseCopy, updateFunction, currentAttempt + 1);
            } finally {
                if (!committed) {
                    session.abortTransaction();
                }
            }
        }
    }

    // Applies linear backoff to the current thread + some random jitter
    // Exponential backoff was way too slow, increasing the milliseconds far too fast
    // Linear backoff was selected to be more predictable and less extreme
    private static void applyBackoff(long attempt) {
        try {
            long linearBackoff = BASE_BACKOFF_MS + (LINEAR_BACKOFF_MS * attempt);
            // add the jitter (+- 25%)
            long half = linearBackoff / 2;
            long jitter = RANDOM.nextLong(half) - (half / 2);

            long backoff = linearBackoff + jitter;
            Thread.sleep(backoff);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Operation interrupted during backoff", e);
        }
    }

    private static boolean isWriteConflict(MongoCommandException e) {
        return e.getErrorCode() == WRITE_CONFLICT_ERROR;
    }

    private static void logWriteConflict(int currentAttempt) {
        DataStoreSource.getColorLogger().debug(
            "Write conflict detected, attempt " + (currentAttempt + 1) + " of " + DEFAULT_MAX_RETRIES
        );
    }
}
