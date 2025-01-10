package com.kamikazejam.datastore.connections.monitor;

import com.google.common.base.Preconditions;
import com.kamikazejam.datastore.connections.storage.mongo.MongoStorage;
import com.mongodb.event.*;
import org.jetbrains.annotations.NotNull;

// Previously this class had used heartbeat events, but the first heartbeat was sent 10 seconds after initial connection.
// That was adding 10 seconds to the ttl of the server, which was unacceptable.
/**
 * Monitors the MongoDB connection status and logs changes, we use cluster listeners to get the current status of the MongoDB connection.
 */
public class MongoMonitor implements ClusterListener, ServerMonitorListener {
    private final @NotNull MongoStorage service;
    public MongoMonitor(@NotNull MongoStorage service) {
        Preconditions.checkNotNull(service, "MongoStorage cannot be null");
        this.service = service;
    }

    @Override
    public void clusterDescriptionChanged(@NotNull ClusterDescriptionChangedEvent event) {
        boolean wasConnected = event.getPreviousDescription().hasWritableServer();
        boolean isConnected = event.getNewDescription().hasWritableServer();

        // Update the ping value
        if (!event.getNewDescription().getServerDescriptions().isEmpty()) {
            this.service.setMongoPingNS(event.getNewDescription().getServerDescriptions());
        }

        if (!wasConnected && isConnected) {
            if (!this.service.isMongoInitConnect()) {
                this.service.setMongoInitConnect(true);
                this.service.info("&aMongoDB initial connection succeeded.");
                this.service.info("&aPlayers may now join the server.");
            } else {
                this.service.debug("MongoDB connection restored");
            }
            this.service.setMongoConnected(true);
        } else if (wasConnected && !isConnected) {
            this.service.info("MongoDB connection lost");
            this.service.setMongoConnected(false);
        }
    }

    @Override
    public void clusterClosed(@NotNull ClusterClosedEvent event) {
        this.service.setMongoConnected(false);
    }

    @Override
    public void serverHeartbeatSucceeded(@NotNull ServerHeartbeatSucceededEvent event) {
        // Attempt to use the current client's cluster description to make sure the ping is accurate
        this.service.setMongoPingNS(this.service.getMongoClient().getClusterDescription().getServerDescriptions());
    }
}