package com.kamikazejam.datastore.base.log;

import com.kamikazejam.datastore.DataStoreSource;
import org.bukkit.plugin.Plugin;

public class PluginLogger extends LoggerService {
    public PluginLogger() {}

    @Override
    public String getLoggerName() {
        return "DataStore";
    }

    @Override
    public Plugin getPlugin() {
        return DataStoreSource.get();
    }

    @Override
    public boolean isDebug() {
        return DataStoreSource.isDebug();
    }
}
