package com.kamikazejam.datastore.command.sub;

import com.kamikazejam.datastore.DataStoreAPI;
import com.kamikazejam.datastore.DataStoreSource;
import com.kamikazejam.datastore.command.SubCommand;
import com.kamikazejam.datastore.connections.storage.StorageService;
import com.kamikazejam.datastore.database.DatabaseRegistration;
import com.kamikazejam.datastore.util.Color;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class CmdDatabases extends SubCommand {
    public CmdDatabases() {}

    @Override
    public @NotNull String getName() {
        return "databases";
    }

    @Override
    public @Nullable String getPermission() {
        return "datastore.command.databases";
    }

    @Override
    public @NotNull String getArgsDescription() {
        return "";
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        sender.sendMessage(Color.t("&7***** &6Store Database &7*****"));
        StorageService store = DataStoreSource.getStorageService();
        sender.sendMessage(Color.t("&7Storage Service: " + ((store.canCache()) ? "&aConnected" : "&cDisconnected")));
        sender.sendMessage(Color.t("&7Databases:"));
        DataStoreAPI.getDatabases().values().stream()
                .filter(Objects::nonNull)
                .map(DatabaseRegistration::getDatabaseName)
                .forEach((n) -> sender.sendMessage(Color.t("&7 - " + n)));
    }
}
