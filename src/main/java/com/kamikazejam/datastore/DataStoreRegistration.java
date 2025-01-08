package com.kamikazejam.datastore;

import com.google.common.base.Preconditions;
import com.kamikazejam.datastore.base.Cache;
import com.kamikazejam.datastore.base.StoreCache;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

@Getter @SuppressWarnings("unused")
public class DataStoreRegistration {
    private final @NotNull JavaPlugin plugin;
    /**
     * The full database name as it would appear in MongoDB
     * This includes the DataStore prefix, described in {@link DataStoreAPI#getFullDatabaseName(String)} (String)}
     * All plugin caches will be stored in this database as collections
     */
    private final String databaseName;
    private final String dbNameShort;

    // package-private because DataStore is the only one allowed to create this
    DataStoreRegistration(@NotNull JavaPlugin plugin, @NotNull String dbNameShort) {
        Preconditions.checkNotNull(plugin);
        Preconditions.checkNotNull(dbNameShort);
        this.plugin = plugin;
        this.dbNameShort = dbNameShort;
        this.databaseName = DataStoreAPI.getFullDatabaseName(dbNameShort);
    }

    private final List<Cache<?,?>> caches = new ArrayList<>();
    public void registerCache(Class<? extends StoreCache<?,?>> clazz) {
        // Find a constructor that takes a DataStoreRegistration
        try {
            // Find the constructor (regardless of visibility)
            Constructor<? extends StoreCache<?,?>> constructor = clazz.getDeclaredConstructor(DataStoreRegistration.class);
            constructor.setAccessible(true);
            StoreCache<?,?> cache = constructor.newInstance(this);
            this.caches.add(cache);
            DataStoreSource.getStorageService().onRegisteredCache(cache);
            cache.getLoggerService().info("Cache Registered.");
        } catch (NoSuchMethodException ex1) {
            DataStoreSource.error("Failed to register cache " + clazz.getName() + " - No constructor that takes a DataStoreRegistration");
        } catch (Throwable t) {
            DataStoreSource.error("Failed to register cache " + clazz.getName() + " - " + t.getClass().getName() + ": " + t.getMessage());
        }
    }

    public void shutdown() {
        caches.forEach(Cache::shutdown);
        caches.clear();
    }
}
