package com.kamikazejam.datastore.base.log;

import com.kamikazejam.datastore.DataStoreAPI;
import com.kamikazejam.datastore.base.Cache;
import org.bukkit.plugin.Plugin;

public class CacheLoggerService extends LoggerService {

    protected final Cache<?, ?> cache;

    public CacheLoggerService(Cache<?, ?> cache) {
        this.cache = cache;
    }

    @Override
    public boolean isDebug() {
        return DataStoreAPI.isDebug();
    }

    @Override
    public String getLoggerName() {
        return "C: " + cache.getName();
    }

    @Override
    public Plugin getPlugin() {
        return cache.getPlugin();
    }
}
