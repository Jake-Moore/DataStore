package com.kamikazejam.datastore;

import com.google.common.base.Preconditions;
import com.kamikazejam.datastore.base.Cache;
import com.kamikazejam.datastore.base.exception.DuplicateCacheException;
import com.kamikazejam.datastore.base.exception.DuplicateDatabaseException;
import com.kamikazejam.datastore.database.DatabaseRegistration;
import lombok.Getter;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Main class of DataStore. This project does not work as a plugin, only a shade-able library.
 */
@SuppressWarnings("unused")
public class DataStoreAPI {
    // ------------------------------------------------------ //
    // Prefix Methods                                         //
    // ------------------------------------------------------ //
    private static final AtomicReference<@NotNull String> dataStorePrefix = new AtomicReference<>("global");
    public static void setDataStorePrefix(@NotNull String prefix) {
        Preconditions.checkNotNull(prefix);
        dataStorePrefix.set(prefix);
    }
    public static @NotNull String getDataStorePrefix() {
        return dataStorePrefix.get();
    }



    // ------------------------------------------------------ //
    // Registration Methods                                   //
    // ------------------------------------------------------ //
    /**
     * Register your plugin and reserve a database name for your plugin's caches.
     * @return Your DataStoreRegistration (to be passed into your cache constructors)
     * @throws DuplicateDatabaseException - if this databaseName is already in use
     */
    public static DataStoreRegistration register(@NotNull JavaPlugin plugin, @NotNull String databaseName) throws DuplicateDatabaseException {
        Preconditions.checkNotNull(databaseName);

        registerDatabase(plugin, getFullDatabaseName(databaseName));
        return new DataStoreRegistration(plugin, databaseName);
    }



    // ------------------------------------------------------ //
    // Database Methods                                       //
    // ------------------------------------------------------ //
    // Key is databaseName stored lowercase for uniqueness checks
    @Getter
    private static final ConcurrentMap<String, DatabaseRegistration> databases = new ConcurrentHashMap<>();

    private static void registerDatabase(@NotNull Plugin plugin, @NotNull String databaseName) throws DuplicateDatabaseException {
        @Nullable DatabaseRegistration registration = getDatabaseRegistration(databaseName);
        if (registration != null) {
            throw new DuplicateDatabaseException(registration, plugin);
        }
        databases.put(databaseName.toLowerCase(), new DatabaseRegistration(databaseName, plugin));
    }
    private static @Nullable DatabaseRegistration getDatabaseRegistration(@NotNull String databaseName) {
        return databases.get(databaseName.toLowerCase());
    }

    /**
     * Check if a database name is already registered
     */
    public static boolean isDatabaseNameRegistered(String databaseName) {
        return databases.containsKey(databaseName.toLowerCase());
    }
    /**
     * Adds the DataStore prefix ({@link DataStoreAPI#getDataStorePrefix()}) and a '_' char to the beginning of the dbName,
     * to allow multiple servers running DataStore to operate on the same MongoDB instance
     * (Assuming the prefix is unique to each server)
     */
    public static @NotNull String getFullDatabaseName(String dbName) {
        // Just in case, don't add the prefix twice
        final String prefix = dataStorePrefix.get();
        if (dbName.startsWith(prefix + "_")) {
            return dbName;
        }
        return prefix + "_" + dbName;
    }



    // ------------------------------------------------------ //
    // Cache Methods                                          //
    // ------------------------------------------------------ //
    @Getter
    private static final ConcurrentMap<String, Cache<?, ?>> caches = new ConcurrentHashMap<>();

    /**
     * Get a cache by name
     *
     * @param name Name of the cache
     * @return The Cache
     */
    public static @Nullable Cache<?,?> getCache(String name) {
        return caches.get(convertCacheName(name));
    }

    /**
     * Register a cache w/ a hook
     *
     * @param cache {@link Cache}
     */
    public static void saveCache(Cache<?, ?> cache) throws DuplicateCacheException {
        if (caches.containsKey(convertCacheName(cache.getName()))) {
            throw new DuplicateCacheException(cache);
        }
        caches.put(convertCacheName(cache.getName()), cache);
    }

    /**
     * Unregister a cache w/ a hook
     */
    public static void removeCache(Cache<?, ?> cache) {
        caches.remove(convertCacheName(cache.getName()));
    }

    /**
     * Removes all spaces from the name and converts it to lowercase.
     */
    public static String convertCacheName(String name) {
        return name.toLowerCase().replace(" ", "");
    }


    private static List<Cache<?,?>> _sortedCachesReversed = null;
    /**
     * Retrieve the caches in sorted order by dependencies (load order)
     */
    public static @NotNull List<Cache<?,?>> getSortedCachesByDependsReversed() {
        if (_sortedCachesReversed != null && !hasBeenModified()) {
            return _sortedCachesReversed;
        }
        _sortedCachesReversed = caches.values().stream().sorted().collect(Collectors.toList());
        return _sortedCachesReversed;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean hasBeenModified() {
        return caches.size() != _sortedCachesReversed.size();
    }


    // ------------------------------------------------------ //
    // Miscellaneous Methods                                  //
    // ------------------------------------------------------ //
    /**
     * @return if DataStore is in debug mode
     */
    public static boolean isDebug() {
        return DataStoreSource.isDebug();
    }
}
