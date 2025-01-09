package com.kamikazejam.datastore;

import com.kamikazejam.datastore.base.mode.StorageMode;
import com.kamikazejam.datastore.command.DataStoreCommand;
import com.kamikazejam.datastore.connections.storage.StorageService;
import com.kamikazejam.datastore.mode.profile.listener.ProfileListener;
import lombok.Getter;
import lombok.SneakyThrows;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

@SuppressWarnings("unused")
public class DataStoreSource {
    private static final String storeConfigName = "datastore.yml";
    private static @Nullable DataStorePlugin pluginSource;
    private static boolean enabled = false;
    @Getter
    private static long onEnableTime = 0;

    // Modes
    @Getter
    private static StorageMode storageMode;
    // Server Identification
    @Getter
    private static String storeDbPrefix;

    /**
     * @return true IFF a plugin source was NEEDED and used for registration
     */
    @SneakyThrows
    @SuppressWarnings("UnusedReturnValue")
    public static boolean onEnable(@NotNull DataStorePlugin pl) {
        if (enabled) {
            return false;
        }
        pluginSource = pl;
        enabled = true;

        // ----------------------------- //
        //      DataStore onEnable      //
        // ----------------------------- //
        // Load Plugin Modes
        storageMode = StorageMode.MONGODB; // For now, if we add more db types, we can change this
        info("Running in " + storageMode + " storage mode.");

        // Load DataStore prefix
        FileConfiguration config = getConfig();
        storeDbPrefix = config.getString("datastore-database-prefix", "global");

        // Enable Services
        storageMode.enableServices();

        // Load Commands
        pluginSource.getCommand("datastore").setExecutor(new DataStoreCommand());

        // Register ProfileListener
        pl.getServer().getPluginManager().registerEvents(new ProfileListener(), pl);

        onEnableTime = System.currentTimeMillis();
        return true;
    }

    /**
     * @return true IFF this call triggered the singleton disable sequence, false it already disabled
     */
    @SuppressWarnings("UnusedReturnValue")
    public static boolean onDisable() {
        if (!enabled) {
            return false;
        }

        // Shutdown Services
        storageMode.disableServices();

        // Set to disabled
        boolean prev = enabled;
        enabled = false;
        return prev;
    }

    public static @NotNull DataStorePlugin get() {
        if (pluginSource == null) {
            throw new RuntimeException("Plugin source not set");
        }
        return pluginSource;
    }

    public static void info(@NotNull String msg) {
        if (pluginSource == null) {
            System.out.println("[INFO] " + msg);
        } else {
            pluginSource.getLogger().info(msg);
        }
    }

    public static void warning(@NotNull String msg) {
        if (pluginSource == null) {
            System.out.println("[WARNING] " + msg);
        } else {
            pluginSource.getLogger().warning(msg);
        }
    }

    public static void error(@NotNull String msg) {
        if (pluginSource == null) {
            System.out.println("[ERROR] " + msg);
        } else {
            pluginSource.getLogger().severe(msg);
        }
    }

    // KamiConfig access of
    private static FileConfiguration storeCfg = null;
    public static @NotNull FileConfiguration getConfig() {
        final JavaPlugin plugin = get();
        if (storeCfg == null) {
            storeCfg = createConfig(plugin);
        }
        return storeCfg;
    }

    @NotNull
    @SneakyThrows
    private static FileConfiguration createConfig(@NotNull JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), storeConfigName);
        if (!file.exists()) {
            boolean ignored = file.getParentFile().mkdirs();
            plugin.saveResource(storeConfigName, false);
        }
        FileConfiguration config = new YamlConfiguration();
        config.load(file);
        return config;
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
