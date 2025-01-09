package com.kamikazejam.datastore.command.sub;

import com.kamikazejam.datastore.DataStoreSource;
import com.kamikazejam.datastore.command.SubCommand;
import com.kamikazejam.datastore.util.Color;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CmdInfo extends SubCommand {
    public CmdInfo() {}

    @Override
    public @NotNull String getName() {
        return "info";
    }

    @Override
    public @Nullable String getPermission() {
        return "datastore.command.info";
    }

    @Override
    public @NotNull String getArgsDescription() {
        return "";
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        sender.sendMessage(Color.t("&7--- &6DataStore Information&7---"));
        sender.sendMessage(Color.t("&7Database Prefix:"));
        sender.sendMessage(Color.t("  &6" + DataStoreSource.getStoreDbPrefix()));
        sender.sendMessage(Color.t("&7Storage Service:"));
        sender.sendMessage(Color.t("  &6Name: " + DataStoreSource.getStorageMode().name()));
        String r = (DataStoreSource.getStorageService().canCache()) ? "&aYes" : "&cNo";
        sender.sendMessage(Color.t("  &6Ready: " + r));
    }
}
