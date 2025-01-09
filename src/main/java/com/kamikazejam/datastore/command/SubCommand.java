package com.kamikazejam.datastore.command;

import com.kamikazejam.datastore.util.Color;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class SubCommand {
    public abstract @Nullable String getPermission();
    public abstract @NotNull String getName();
    public abstract @NotNull String getArgsDescription();

    public void sendNoPerm(CommandSender sender) {
        sender.sendMessage(Color.t("&cYou do not have permission to use this command."));
    }

    public void sendUsage(CommandSender sender) {
        sender.sendMessage(Color.t("&cNot enough command input. &eYou should use it like this:"));
        sender.sendMessage(Color.t("&b/datastore " + this.getName() + "&3 " + this.getArgsDescription()));
    }

    /**
     * @param args The args of this subcommand, not including this command's name
     */
    public abstract void execute(@NotNull CommandSender sender, @NotNull String[] args);

    /**
     * @param args The args of this subcommand, not including this command's name
     */
    @NotNull
    public List<String> getTabCompletions(@NotNull CommandSender sender, @NotNull String[] args) {
        return new ArrayList<>();
    }
}
