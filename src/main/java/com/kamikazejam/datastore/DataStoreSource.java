package com.kamikazejam.datastore;

import com.kamikazejam.datastore.base.mode.StorageMode;
import com.kamikazejam.datastore.command.DataStoreCommand;
import com.kamikazejam.datastore.connections.storage.StorageService;
import com.kamikazejam.datastore.mode.profile.listener.ProfileListener;
import com.kamikazejam.kamicommon.KamiPlugin;
import com.kamikazejam.kamicommon.SpigotUtilsSource;
import com.kamikazejam.kamicommon.configuration.spigot.KamiConfig;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

@SuppressWarnings("unused")
public class DataStoreSource {
    private static @Nullable KamiPlugin pluginSource;
    private static boolean enabled = false;
    @Getter private static long onEnableTime = 0;
    private static DataStoreCommand command;

    // Modes
    @Getter private static StorageMode storageMode;
    // Server Identification
    @Getter private static String storeDbPrefix;

    /**
     * @return true IFF a plugin source was NEEDED and used for registration
     */
    @SuppressWarnings("UnusedReturnValue")
    public static boolean onEnable(@NotNull KamiPlugin plugin) {
        if (enabled) { return false; }
        pluginSource = plugin;
        enabled = true;

        // ----------------------------- //
        //      DataStore onEnable      //
        // ----------------------------- //
        // Load Plugin Modes
        storageMode = StorageMode.MONGODB; // For now, if we add more db types, we can change this
        info("Running in " + storageMode + " storage mode.");

        // Load DataStore prefix
        KamiConfig storeConfig = new KamiConfig(plugin, new File(plugin.getDataFolder(), "datastore.yml"), true);
        storeDbPrefix = storeConfig.getString("datastore-database-prefix", "global");

        // Enable Services
        storageMode.enableServices();

        // Load Commands
        command = new DataStoreCommand();
        command.registerCommand(plugin);

        // Register ProfileListener
        plugin.getServer().getPluginManager().registerEvents(new ProfileListener(), plugin);

        onEnableTime = System.currentTimeMillis();
        return true;
    }

    /**
     * @return true IFF this call triggered the singleton disable sequence, false it already disabled
     */
    @SuppressWarnings("UnusedReturnValue")
    public static boolean onDisable() {
        if (!enabled) { return false; }

        // Unload Commands
        if (command != null) {
            command.unregisterCommand();
        }

        // Shutdown Services
        storageMode.disableServices();

        SpigotUtilsSource.onDisable();

        // Set to disabled
        boolean prev = enabled;
        enabled = false;
        return prev;
    }

    public static @NotNull KamiPlugin get() {
        if (pluginSource == null) {
            throw new RuntimeException("Plugin source not set");
        }
        return pluginSource;
    }

    public static void info(@NotNull String msg) {
        if (pluginSource == null) {
            System.out.println("[INFO] " + msg);
        }else {
            pluginSource.getLogger().info(msg);
        }
    }
    public static void warning(@NotNull String msg) {
        if (pluginSource == null) {
            System.out.println("[WARNING] " + msg);
        }else {
            pluginSource.getLogger().warning(msg);
        }
    }
    public static void error(@NotNull String msg) {
        if (pluginSource == null) {
            System.out.println("[ERROR] " + msg);
        }else {
            pluginSource.getLogger().severe(msg);
        }
    }

    // KamiConfig access of datastore.yml
    private static KamiConfig kamiConfig = null;
    public static @NotNull KamiConfig getConfig() {
        final JavaPlugin plugin = get();
        if (kamiConfig == null) {
            kamiConfig = new KamiConfig(plugin, new File(plugin.getDataFolder(), "datastore.yml"), true, true);
        }
        return kamiConfig;
    }

    /**
     * @return If the plugin has debug logging enabled
     */
    public static boolean isDebug() {
        return getConfig().getBoolean("debug", false);
    }

    // --------------------------------------------------------------------------------------- //
    //                                  Service Accessors                                      //
    // --------------------------------------------------------------------------------------- //

    public static @NotNull StorageService getStorageService() {
        return storageMode.getStorageService();
    }
}
