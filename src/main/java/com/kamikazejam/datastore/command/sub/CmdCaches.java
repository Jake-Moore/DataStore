package com.kamikazejam.datastore.command.sub;

import com.kamikazejam.datastore.DataStoreAPI;
import com.kamikazejam.datastore.base.Cache;
import com.kamikazejam.datastore.command.SubCommand;
import com.kamikazejam.datastore.util.Color;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CmdCaches extends SubCommand {
    public CmdCaches() {}

    @Override
    public @NotNull String getName() {
        return "caches";
    }

    @Override
    public @Nullable String getPermission() {
        return "datastore.command.caches";
    }

    @Override
    public @NotNull String getArgsDescription() { return ""; }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        sender.sendMessage(Color.t("&7***** &6Store Caches &7*****"));
        for (Cache<?,?> c : DataStoreAPI.getCaches().values()) {
            sender.sendMessage(Color.t("&7" + c.getName() + " - " + c.getLocalCacheSize() + " local objects"));
        }
    }
}
