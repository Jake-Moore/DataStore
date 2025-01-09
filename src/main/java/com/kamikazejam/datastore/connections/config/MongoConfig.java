package com.kamikazejam.datastore.connections.config;

import com.kamikazejam.datastore.DataStoreSource;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
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
        FileConfiguration config = DataStoreSource.getConfig();
        String uri = config.getString("connections.MONGODB.uri", "mongodb://localhost:27017");
        return conf = new MongoConfig(uri);
    }
}
