package com.kamikazejam.datastore.base.mode

import com.google.common.base.Preconditions
import com.kamikazejam.datastore.DataStoreSource
import com.kamikazejam.datastore.connections.storage.StorageService
import com.kamikazejam.datastore.connections.storage.mongo.MongoStorage
import org.bukkit.Bukkit

// Abstraction layer for adding different storage modes in the future
enum class StorageMode {
    MONGODB;

    fun enableServices() {
        // Enable Storage Service (just fetch storageService)
        when (this) {
            MONGODB -> {
                val storage = MongoStorage()
                if (!storage.start()) {
                    DataStoreSource.get().logger.severe("Failed to start MongoStorage, shutting down...")
                    Bukkit.shutdown()
                }
                mongoStorage = storage
            }
        }
    }

    val storageService: StorageService
        get() = when (this) {
            MONGODB -> getMongoStorage()
        }

    // ---------------------------------------------------------------------------- //
    //                         STORAGE SERVICE MANAGEMENT                           //
    // ---------------------------------------------------------------------------- //
    private var mongoStorage: MongoStorage? = null
    private fun getMongoStorage(): MongoStorage {
        Preconditions.checkState(this == MONGODB, "MongoStorage is only available in MONGODB storage mode")
        return mongoStorage ?: throw IllegalStateException("MongoStorage is not initialized")
    }

    fun disableServices() {
        mongoStorage?.shutdown()
        mongoStorage = null
    }
}
