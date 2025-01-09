package com.kamikazejam.datastore.command.sub;

import com.kamikazejam.datastore.DataStoreAPI;
import com.kamikazejam.datastore.DataStoreSource;
import com.kamikazejam.datastore.connections.storage.StorageService;
import com.kamikazejam.datastore.database.DatabaseRegistration;
import com.kamikazejam.datastore.util.Color;

import java.util.Objects;

public class CmdDatabases extends KamiCommand {
    public CmdDatabases() {
        addAliases("databases", "database");

        addRequirements(RequirementHasPerm.get("datastore.command.databases"));
    }

    @Override
    public void perform() {
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
