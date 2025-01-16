package com.kamikazejam.datastore.connections.config

import com.kamikazejam.datastore.DataStoreSource

// Private constructor - fetch the MongoConf Singleton via static methods
class MongoConfig private constructor(val uri: String) {
    companion object {
        private var conf: MongoConfig? = null

        fun get(): MongoConfig {
            if (conf != null) {
                return conf!!
            }

            // Load Config Values
            val config = DataStoreSource.config
            val uri = config.getString("connections.MONGODB.uri", "mongodb://localhost:27017")
            return MongoConfig(uri).also { conf = it }
        }
    }
}
