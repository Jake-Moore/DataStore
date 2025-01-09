package com.kamikazejam.datastore.command;

import com.kamikazejam.datastore.util.Color;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class SubCommand implements TabExecutor {
    public abstract @NotNull List<String> getAliases();
    public abstract @Nullable String getPermission();
    public abstract @NotNull String getName();
    public abstract @NotNull String getArgsDescription();

    public void sendNoPerm(CommandSender sender) {
        sender.sendMessage(Color.t("&cYou do not have permission to use this command."));
    }
}
