package com.kamikazejam.datastore.connections.monitor

import com.google.common.base.Preconditions
import com.kamikazejam.datastore.connections.storage.mongo.MongoStorage
import com.mongodb.event.ClusterClosedEvent
import com.mongodb.event.ClusterDescriptionChangedEvent
import com.mongodb.event.ClusterListener
import com.mongodb.event.ServerHeartbeatSucceededEvent
import com.mongodb.event.ServerMonitorListener

// Previously this class had used heartbeat events, but the first heartbeat was sent 10 seconds after initial connection.
// That was adding 10 seconds to the ttl of the server, which was unacceptable.
/**
 * Monitors the MongoDB connection status and logs changes, we use cluster listeners to get the current status of the MongoDB connection.
 */
class MongoMonitor(service: MongoStorage) : ClusterListener, ServerMonitorListener {
    private val service: MongoStorage

    init {
        Preconditions.checkNotNull(service, "MongoStorage cannot be null")
        this.service = service
    }

    override fun clusterDescriptionChanged(event: ClusterDescriptionChangedEvent) {
        val wasConnected = event.previousDescription.hasWritableServer()
        val isConnected = event.newDescription.hasWritableServer()

        // Update the ping value
        if (event.newDescription.serverDescriptions.isNotEmpty()) {
            service.setMongoPingNS(event.newDescription.serverDescriptions)
        }

        if (!wasConnected && isConnected) {
            if (!service.mongoInitConnect) {
                service.mongoInitConnect = true
                service.info("&aMongoDB initial connection succeeded.")
                service.info("&aPlayers may now join the server.")
            } else {
                service.debug("MongoDB connection restored")
            }
            service.mongoConnected = true
        } else if (wasConnected && !isConnected) {
            service.info("MongoDB connection lost")
            service.mongoConnected = false
        }
    }

    override fun clusterClosed(event: ClusterClosedEvent) {
        service.mongoConnected = false
    }

    override fun serverHeartbeatSucceeded(event: ServerHeartbeatSucceededEvent) {
        val client = service.mongoClient
        if (client != null) {
            // Attempt to use the current client's cluster description to make sure the ping is accurate
            service.setMongoPingNS(client.getClusterDescription().serverDescriptions)
        }
    }
}