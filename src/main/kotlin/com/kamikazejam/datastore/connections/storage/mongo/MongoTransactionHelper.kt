package com.kamikazejam.datastore.connections.storage.mongo

import com.google.common.base.Preconditions
import com.kamikazejam.datastore.DataStoreSource
import com.kamikazejam.datastore.base.Collection
import com.kamikazejam.datastore.base.exception.update.TransactionRetryLimitExceededException
import com.kamikazejam.datastore.base.exception.update.UpdateException
import com.kamikazejam.datastore.base.metrics.MetricsListener
import com.kamikazejam.datastore.base.serialization.SerializationUtil.getSerialNameForID
import com.kamikazejam.datastore.base.serialization.SerializationUtil.getSerialNameForVersion
import com.kamikazejam.datastore.store.Store
import com.kamikazejam.datastore.util.DataStoreFileLogger
import com.mongodb.MongoCommandException
import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.ClientSession
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoCollection
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import java.util.*
import java.util.logging.Level

@Suppress("MemberVisibilityCanBePrivate")
object MongoTransactionHelper {
    private val RANDOM = Random()
    var DEFAULT_MAX_RETRIES: Int = 50 // very gracious amount of retries (since we have backoff)
    var LOG_WRITE_CONFLICT_AFTER_ATTEMPT: Int = 10 // only log after this many attempts
    private const val WRITE_CONFLICT_ERROR = 112

    // Minimum and maximum backoff values to prevent extremes
    var MIN_BACKOFF_MS: Long = 50
    var MAX_BACKOFF_MS: Long = 2000
    var PING_MULTIPLIER: Double = 2.0 // Base multiplier for ping time
    var ATTEMPT_MULTIPLIER: Double = 1.5 // How much to increase per attempt

    /**
     * Execute a MongoDB document update with retries and version checking
     * @throws UpdateException for fail states
     */
    @Throws(UpdateException::class)
    suspend fun <K : Any, X : Store<X, K>> executeUpdate(
        mongoClient: MongoClient,
        mongoColl: MongoCollection<X>,
        collection: Collection<K, X>,
        store: X,
        updateFunction: (X) -> X
    ): X {
        Preconditions.checkNotNull(mongoClient, "MongoClient cannot be null")
        Preconditions.checkNotNull(mongoColl, "MongoDB Collection cannot be null")
        Preconditions.checkNotNull(collection, "Collection cannot be null")
        Preconditions.checkNotNull(store, "Store cannot be null")
        Preconditions.checkNotNull(updateFunction, "Update function cannot be null")

        // Will either return the store, or throw an UpdateException
        val updatedStore: X = retryExecutionHelper(
            mongoClient,
            mongoColl,
            collection,
            store,
            updateFunction,
            0
        )

        // Ensure our local cache is updated with the new updated store
        collection.updateStoreFromNewer(store, updatedStore)
        return updatedStore
    }

    // If no error is thrown, this method succeeded
    @Throws(UpdateException::class)
    private suspend fun <K : Any, X : Store<X, K>> retryExecutionHelper(
        mongoClient: MongoClient,
        mongoColl: MongoCollection<X>,
        collection: Collection<K, X>,
        store: X,
        updateFunction: (X) -> X,
        currentAttempt: Int
    ): X {
        // Quit if we've run out of attempts
        if (currentAttempt >= DEFAULT_MAX_RETRIES) {
            DataStoreSource.metricsListeners.forEach(MetricsListener::onUpdateTransactionLimitReached)
            throw TransactionRetryLimitExceededException("Failed to execute update after $DEFAULT_MAX_RETRIES attempts.")
        }

        // Apply exponential backoff if this isn't our first attempt
        if (currentAttempt > 0) {
            applyBackoff(currentAttempt.toLong())
        }

        // Log the Metric
        DataStoreSource.metricsListeners.forEach(MetricsListener::onTryUpdateTransaction)

        mongoClient.startSession().use { session ->
            session.startTransaction()
            var sessionResolved = false
            try {
                // Fetch Version prior to updates
                val result = processTransaction(mongoColl, session, collection, store, updateFunction)

                // Handle Fail State
                if (result.dbStore != null) {
                    session.abortTransaction()
                    sessionResolved = true
                    // Retry with the new database store
                    return retryExecutionHelper(
                        mongoClient,
                        mongoColl,
                        collection,
                        result.dbStore,
                        updateFunction,
                        currentAttempt + 1
                    )
                }

                sessionResolved = true // resolve before call, in case it fails partial
                // Success - return true
                session.commitTransaction()
                return checkNotNull(result.store)
            } catch (uE: UpdateException) {
                throw uE
            } catch (mE: MongoCommandException) {
                if (isWriteConflict(mE)) {
                    logWriteConflict(currentAttempt, mE, collection, store)
                    // For write conflicts, retry with same working copy
                    return retryExecutionHelper(
                        mongoClient,
                        mongoColl,
                        collection,
                        store,
                        updateFunction,
                        currentAttempt + 1
                    )
                }
                throw UpdateException("Failed to execute MongoDB update", mE)
            } catch (e: Exception) {
                DataStoreFileLogger.warn("Failed to execute MongoDB update", e)
                throw UpdateException("Failed to execute MongoDB update", e)
            } finally {
                if (!sessionResolved) {
                    session.abortTransaction()
                }
            }
        }
    }

    private suspend fun <K : Any, X : Store<X, K>> processTransaction(
        mongoColl: MongoCollection<X>,
        session: ClientSession,
        collection: Collection<K, X>,
        store: X,
        updateFunction: (X) -> X,
    ): TransactionResult<K, X> {
        val currentVersion: Long = store.version
        val nextVersion = currentVersion + 1
        val id: String = collection.keyToString(store.id)

        // Apply the Update Function
        //  which MUST (by convention) return a new instance (like a data class copy)
        // Also have the Store class create a copy with the desired version
        val updatedStore: X = updateFunction(store).copyHelper(nextVersion)
        if (updatedStore === store) {
            throw IllegalArgumentException("Update function must return a new instance of the store (like a data class copy)")
        }

        // Validate ID Property
        if (id != collection.keyToString(updatedStore.id)) {
            throw IllegalArgumentException("Updated store failed copy id check! Was: ${updatedStore.id}, Expected: ${store.id}")
        }

        // Validate Incremented Version (Optimistic Versioning)
        if (updatedStore.version != nextVersion) {
            throw IllegalArgumentException("Updated store failed copy version check! Was: ${updatedStore.version}, Expected: $nextVersion")
        }

        val idField = getSerialNameForID(collection)
        val verField = getSerialNameForVersion(collection)
        val result = mongoColl.replaceOne(
            session,
            Filters.and(
                // These two filters act as a sort of compare-and-swap mechanic
                //  inside of this mongo transaction, if these are not met then
                //  the transaction will fail and we will need to retry.
                Filters.eq(idField, id),
                Filters.eq(verField, currentVersion),
            ),
            updatedStore
        )

        // If no documents were modified, then the compare-and-swap failed, we must retry
        if (result.modifiedCount == 0L) {
            DataStoreSource.colorLogger.debug("Failed to update Store in MongoDB Layer (Could not find document with id: '$id' and version: $currentVersion)")

            // If update failed, fetch current version
            val databaseStore: X = mongoColl.find(session).filter(
                Filters.eq(idField, id)
            ).firstOrNull() ?: throw RuntimeException("Entity not found for collection: ${collection.name}, id: $idField -> $id")

            // Update our working copy with latest version and retry
            return TransactionResult(null, databaseStore)
        }

        // Success! Return a successful result
        return TransactionResult(updatedStore, null)
    }

    // Applies linear backoff to the current thread + some random jitter
    // Exponential backoff was way too slow, increasing the milliseconds far too fast
    // Linear backoff was selected to be more predictable and less extreme
    private suspend fun applyBackoff(attempt: Long) {
        // Divide by 2 since this method fetches the round trip time
        val pingNanos = DataStoreSource.storageService.averagePingNanos / 2
        // Convert ping from nanos to ms and apply base multiplier
        val basePingMs = (pingNanos / 1000000) * PING_MULTIPLIER.toLong()


        // Calculate backoff with attempt scaling
        var backoffMs = basePingMs + (basePingMs * ATTEMPT_MULTIPLIER * attempt).toLong()

        // Clamp the value between min and max
        backoffMs = backoffMs.coerceAtLeast(MIN_BACKOFF_MS).coerceAtMost(MAX_BACKOFF_MS)

        // Add jitter (±25%)
        val half = backoffMs / 4 // 25% of total
        val jitter = RANDOM.nextLong(-half, half)

        delay(backoffMs + jitter)
    }

    private fun isWriteConflict(e: MongoCommandException): Boolean {
        return e.errorCode == WRITE_CONFLICT_ERROR
    }


    private fun <K : Any, X : Store<X, K>> logWriteConflict(
        currentAttempt: Int,
        mE: MongoCommandException,
        collection: Collection<K, X>,
        store: X,
    ) {
        if (currentAttempt < LOG_WRITE_CONFLICT_AFTER_ATTEMPT) return

        val msg = "Write Conflict, attempt " + (currentAttempt + 1) + " of " + DEFAULT_MAX_RETRIES + " for coll " + collection.name + " with id " + collection.keyToString(store.id)
        DataStoreFileLogger.log(
            msg,
            mE,
            DataStoreFileLogger.randomWriteExceptionFile,
            Level.INFO
        )
    }
}
