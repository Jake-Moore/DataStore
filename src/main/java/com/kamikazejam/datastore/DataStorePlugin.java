package com.kamikazejam.datastore;

import com.kamikazejam.datastore.base.log.LoggerService;
import com.kamikazejam.datastore.base.log.PluginLogger;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * This class is nothing more than a loader for all DataStore logic
 * It supplies {@link DataStoreSource} with this plugin object so that DataStore can be initialized
 * DataStore can be shaded into your own project, where you'll just have to mirror these method
 *  calls in your own plugin, to initialize DataStore
 */
@SuppressWarnings("unused")
public class DataStorePlugin extends JavaPlugin {
    private final LoggerService colorLogger;
    public DataStorePlugin() {
        this.colorLogger = new PluginLogger(this);
    }

    @Override
    public void onEnable() {
        DataStoreSource.onEnable(this);
    }

    @Override
    public void onDisable() {
        DataStoreSource.onDisable();
    }

    public @NotNull LoggerService getColorLogger() {
        return this.colorLogger;
    }

    public boolean isDebug() {
        return true;
    }
}
