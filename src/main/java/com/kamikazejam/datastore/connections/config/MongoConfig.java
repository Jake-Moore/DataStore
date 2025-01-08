package com.kamikazejam.datastore.connections.config;

import com.kamikazejam.datastore.DataStoreSource;
import com.kamikazejam.kamicommon.configuration.spigot.KamiConfig;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

@Getter
public class MongoConfig {
    private final String uri;

    // Private - fetch the MongoConf Singleton via static methods
    private MongoConfig(String uri) {
        this.uri = uri;
    }

    private static @Nullable MongoConfig conf = null;

    public static MongoConfig get() {
        if (conf != null) {
            return conf;
        }

        // Load Config Values
        KamiConfig config = DataStoreSource.getConfig();
        String uri = config.getString("connections.MONGODB.uri", null);
        return conf = new MongoConfig(uri);
    }
}
