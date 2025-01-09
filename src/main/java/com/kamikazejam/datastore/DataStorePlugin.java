package com.kamikazejam.datastore;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * This class is nothing more than a loader for all DataStore logic
 * It supplies {@link DataStoreSource} with this plugin object so that DataStore can be initialized
 * DataStore can be shaded into your own project, where you'll just have to mirror these method
 *  calls in your own plugin, to initialize DataStore
 */
@SuppressWarnings("unused")
public class DataStorePlugin extends JavaPlugin {
    public DataStorePlugin() {}

    @Override
    public void onEnable() {
        DataStoreSource.onEnable(this);
    }

    @Override
    public void onDisable() {
        DataStoreSource.onDisable();
    }
}
