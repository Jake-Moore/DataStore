package com.kamikazejam.datastore.connections.storage.mongo

import com.google.common.base.Preconditions
import com.kamikazejam.datastore.DataStoreSource
import com.kamikazejam.datastore.base.Collection
import com.kamikazejam.datastore.base.Store
import com.kamikazejam.datastore.connections.storage.exception.TransactionRetryLimitExceededException
import com.kamikazejam.datastore.util.DataStoreFileLogger
import com.kamikazejam.datastore.util.JacksonUtil
import com.kamikazejam.datastore.util.JacksonUtil.ID_FIELD
import com.kamikazejam.datastore.util.JacksonUtil.VERSION_FIELD
import com.kamikazejam.datastore.util.JacksonUtil.serializeValue
import com.mongodb.MongoCommandException
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Filters
import org.bson.Document
import java.util.*
import java.util.function.Consumer

@Suppress("MemberVisibilityCanBePrivate")
object MongoTransactionHelper {
    private val RANDOM = Random()
    var DEFAULT_MAX_RETRIES: Int = 50 // very gracious amount of retries (since we have backoff)
    private const val WRITE_CONFLICT_ERROR = 112

    // Minimum and maximum backoff values to prevent extremes
    var MIN_BACKOFF_MS: Long = 50
    var MAX_BACKOFF_MS: Long = 2000
    var PING_MULTIPLIER: Double = 2.0 // Base multiplier for ping time
    var ATTEMPT_MULTIPLIER: Double = 1.5 // How much to increase per attempt

    /**
     * Execute a MongoDB document update with retries and version checking
     * @param mongoClient The MongoDB client
     * @param mongoColl The MongoDB collection
     * @param collection The collection containing the document
     * @param originalStore The original store to update
     * @param updateFunction The function to apply updates
     * @return Whether the update was successful
     */
    fun <K : Any, X : Store<X, K>> executeUpdate(
        mongoClient: MongoClient,
        mongoColl: MongoCollection<Document>,
        collection: Collection<K, X>,
        originalStore: X,
        updateFunction: Consumer<X>
    ): Boolean {
        Preconditions.checkNotNull(mongoClient, "MongoClient cannot be null")
        Preconditions.checkNotNull(mongoColl, "MongoDB Collection cannot be null")
        Preconditions.checkNotNull(collection, "Collection cannot be null")
        Preconditions.checkNotNull(originalStore, "Store cannot be null")
        Preconditions.checkNotNull(updateFunction, "Update function cannot be null")

        try {
            // Create working copy that will be updated on each attempt
            val baseCopy = JacksonUtil.deepCopy(originalStore)
            return executeUpdateInternal(
                mongoClient,
                mongoColl,
                collection,
                originalStore,
                baseCopy,
                updateFunction,
                0
            )
        } catch (e: TransactionRetryLimitExceededException) {
            DataStoreFileLogger.warn("Failed to execute MongoDB update in $DEFAULT_MAX_RETRIES attempts")
            return false
        }
    }

    // If no error is thrown, this method succeeded
    @Throws(TransactionRetryLimitExceededException::class)
    private fun <K : Any, X : Store<X, K>> executeUpdateInternal(
        mongoClient: MongoClient,
        mongoColl: MongoCollection<Document>,
        collection: Collection<K, X>,
        originalStore: X,
        baseStore: X,
        updateFunction: Consumer<X>,
        currentAttempt: Int
    ): Boolean {
        var baseCopy = baseStore
        // Quit if we've run out of attempts
        if (currentAttempt >= DEFAULT_MAX_RETRIES) {
            throw TransactionRetryLimitExceededException("Failed to execute update after $DEFAULT_MAX_RETRIES attempts.")
        }

        // Apply exponential backoff if this isn't our first attempt
        if (currentAttempt > 0) {
            applyBackoff(currentAttempt.toLong())
        }

        mongoClient.startSession().use { session ->
            session.startTransaction()
            var committed = false
            try {
                // Clone the base copy for this attempt
                val workingCopy = JacksonUtil.deepCopy(baseCopy)
                workingCopy.readOnly = false

                // Fetch Version prior to updates
                val currentVersion = workingCopy.versionField.getData().get()

                // Apply updates to the copy
                updateFunction.accept(workingCopy)
                // Increment version (Optimistic Versioning)
                workingCopy.versionField.getData().set(currentVersion + 1)

                val id = collection.keyToString(workingCopy.id)
                val doc = JacksonUtil.serializeToDocument(workingCopy)

                // Generate the dbStrings that we need to search for, by mirroring the serialization logic
                // of the FieldWrapper that created the strings
                val idDbString = serializeValue(id)
                val verDbString = serializeValue(currentVersion)

                val result = mongoColl.replaceOne(
                    session,
                    Filters.and(
                        // These two filters act as a sort of compare-and-swap mechanic
                        //  inside of this mongo transaction, if these are not met then
                        //  the transaction will fail and we will need to retry.
                        Filters.eq(ID_FIELD, idDbString),
                        Filters.eq(VERSION_FIELD, verDbString)
                    ),
                    doc
                )

                // If no documents were modified, then the compare-and-swap failed, we must retry
                if (result.modifiedCount == 0L) {
                    DataStoreSource.colorLogger.debug("Failed to update Store in MongoDB Layer (Could not find document with id: '$id' and version: $currentVersion)")

                    // If update failed, fetch current version
                    val currentDoc: Document =
                        mongoColl.find(session).filter(Filters.eq(ID_FIELD, idDbString))
                            .first()
                            ?: throw RuntimeException("Entity not found")

                    // Update our working copy with latest version and retry
                    baseCopy = JacksonUtil.deserializeFromDocument(collection.storeClass, currentDoc)
                    return executeUpdateInternal(
                        mongoClient,
                        mongoColl,
                        collection,
                        originalStore,
                        baseCopy,
                        updateFunction,
                        currentAttempt + 1
                    )
                }

                // Success - update the cached store from our working copy
                workingCopy.readOnly = true
                originalStore.readOnly = false
                collection.updateStoreFromNewer(originalStore, workingCopy)
                collection.cache(originalStore)
                originalStore.readOnly = true

                session.commitTransaction()
                committed = true
                return true
            } catch (t: TransactionRetryLimitExceededException) {
                // re-throw to bubble up
                throw t
            } catch (mE: MongoCommandException) {
                if (isWriteConflict(mE)) {
                    logWriteConflict(currentAttempt)
                    // For write conflicts, retry with same working copy
                    return executeUpdateInternal(
                        mongoClient,
                        mongoColl,
                        collection,
                        originalStore,
                        baseCopy,
                        updateFunction,
                        currentAttempt + 1
                    )
                }
                throw mE
            } catch (e: Exception) {
                DataStoreFileLogger.warn("Failed to execute MongoDB update", e)
                // Generic Exceptions are not a cause for retry, we should log and return failure
                return false
            } finally {
                if (!committed) {
                    session.abortTransaction()
                }
            }
        }
    }

    // Applies linear backoff to the current thread + some random jitter
    // Exponential backoff was way too slow, increasing the milliseconds far too fast
    // Linear backoff was selected to be more predictable and less extreme
    private fun applyBackoff(attempt: Long) {
        // Divide by 2 since this method fetches the round trip time
        val pingNanos = DataStoreSource.storageService.averagePingNanos / 2
        try {
            // Convert ping from nanos to ms and apply base multiplier
            val basePingMs = (pingNanos / 1000000) * PING_MULTIPLIER.toLong()


            // Calculate backoff with attempt scaling
            var backoffMs = basePingMs + (basePingMs * ATTEMPT_MULTIPLIER * attempt).toLong()

            // Clamp the value between min and max
            backoffMs = backoffMs.coerceAtLeast(MIN_BACKOFF_MS).coerceAtMost(MAX_BACKOFF_MS)

            // Add jitter (±25%)
            val half = backoffMs / 4 // 25% of total
            val jitter = RANDOM.nextLong(-half, half)

            Thread.sleep(backoffMs + jitter)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw RuntimeException("Operation interrupted during backoff", e)
        }
    }

    private fun isWriteConflict(e: MongoCommandException): Boolean {
        return e.errorCode == WRITE_CONFLICT_ERROR
    }

    private fun logWriteConflict(currentAttempt: Int) {
        DataStoreSource.colorLogger.debug(
            "Write conflict detected, attempt " + (currentAttempt + 1) + " of " + DEFAULT_MAX_RETRIES
        )
    }
}
