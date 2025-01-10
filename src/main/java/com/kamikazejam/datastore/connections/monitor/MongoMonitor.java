package com.kamikazejam.datastore.connections.monitor;

import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.NotNull;

import com.google.common.base.Preconditions;
import com.kamikazejam.datastore.connections.storage.mongo.MongoStorage;
import com.mongodb.connection.ServerDescription;
import com.mongodb.event.ClusterClosedEvent;
import com.mongodb.event.ClusterDescriptionChangedEvent;
import com.mongodb.event.ClusterListener;
import com.mongodb.event.ServerHeartbeatSucceededEvent;
import com.mongodb.event.ServerMonitorListener;

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
            long pingSumNS = 0;
            for (ServerDescription server : event.getNewDescription().getServerDescriptions()) {
                pingSumNS += server.getRoundTripTimeNanos();
            }
            long pingAvgNS = pingSumNS / event.getNewDescription().getServerDescriptions().size();
            this.service.setMongoPingNS(pingAvgNS);
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
        this.service.setMongoPingNS(event.getElapsedTime(TimeUnit.NANOSECONDS));
    }
}