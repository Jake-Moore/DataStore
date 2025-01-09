package com.kamikazejam.datastore.base.log;

import com.kamikazejam.datastore.DataStorePlugin;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class PluginLogger extends LoggerService {
    private final @NotNull DataStorePlugin plugin;
    public PluginLogger(@NotNull DataStorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getLoggerName() {
        return "DataStore";
    }

    @Override
    public Plugin getPlugin() {
        return plugin;
    }

    @Override
    public boolean isDebug() {
        return plugin.isDebug();
    }
}
