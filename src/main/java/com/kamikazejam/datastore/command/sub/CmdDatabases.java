package com.kamikazejam.datastore.command.sub;

import com.kamikazejam.datastore.DataStoreAPI;
import com.kamikazejam.datastore.DataStoreSource;
import com.kamikazejam.datastore.connections.storage.StorageService;
import com.kamikazejam.datastore.database.DatabaseRegistration;
import com.kamikazejam.kamicommon.command.KamiCommand;
import com.kamikazejam.kamicommon.command.requirement.RequirementHasPerm;
import com.kamikazejam.kamicommon.util.StringUtil;

import java.util.Objects;

public class CmdDatabases extends KamiCommand {
    public CmdDatabases() {
        addAliases("databases", "database");

        addRequirements(RequirementHasPerm.get("datastore.command.databases"));
    }

    @Override
    public void perform() {
        sender.sendMessage(StringUtil.t("&7***** &6Store Database &7*****"));
        StorageService store = DataStoreSource.getStorageService();
        sender.sendMessage(StringUtil.t("&7Storage Service: " + ((store.canCache()) ? "&aConnected" : "&cDisconnected")));
        sender.sendMessage(StringUtil.t("&7Databases:"));
        DataStoreAPI.getDatabases().values().stream()
                .filter(Objects::nonNull)
                .map(DatabaseRegistration::getDatabaseName)
                .forEach((n) -> sender.sendMessage(StringUtil.t("&7 - " + n)));
    }
}
