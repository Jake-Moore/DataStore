package com.kamikazejam.datastore;

import com.kamikazejam.kamicommon.KamiPlugin;

/**
 * This class is nothing more than a loader for all DataStore logic
 * It supplies {@link DataStoreSource} with this plugin object so that DataStore can be initialized
 * DataStore can be shaded into your own project, where you'll just have to mirror these method
 *  calls in your own plugin, to initialize DataStore
 */
@SuppressWarnings("unused")
public class DataStorePlugin extends KamiPlugin {
    @Override
    public void onEnableInner() {
        DataStoreSource.onEnable(this);
    }

    @Override
    public void onDisableInner() {
        DataStoreSource.onDisable();
    }

    @Override
    public boolean isAutoLoadKamiConfig() {
        return false;
    }
}
