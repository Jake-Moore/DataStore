package com.kamikazejam.datastore.command;

import com.kamikazejam.datastore.command.sub.CmdCache;
import com.kamikazejam.datastore.util.Color;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DataStoreCommand implements TabExecutor {
    private final List<SubCommand> subCommands = new ArrayList<>();
    public DataStoreCommand() {
        subCommands.add(new CmdCache());
//        subCommands.add(new CmdCaches());
//        subCommands.add(new CmdDatabases());
//        subCommands.add(new CmdInfo());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("datastore.command")) {
            sender.sendMessage(Color.t("&cYou do not have permission to use this command."));
            return true;
        }

        // Attempt to find the subcommand
        Bukkit.getLogger().info("Provided Args: " + Arrays.toString(args));

        List<SubCommand> subWithAccess = new ArrayList<>();
        for (SubCommand sub : subCommands) {
            if (sub.getPermission() == null || sender.hasPermission(sub.getPermission())) {
                subWithAccess.add(sub);
            }
        }
        if (subWithAccess.isEmpty()) {
            sender.sendMessage(Color.t("&cYou do not have permission to use this command."));
            return true;
        }
        // Send the help menu
        sender.sendMessage(Color.t("&6___[&2Help for command \"datastore\"&6]___"));
        subWithAccess.forEach(sub -> sender.sendMessage(Color.t("&b/datastore " + sub.getName() + "&3 " + sub.getArgsDescription())));
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        Bukkit.getLogger().info("TabComplete Args: " + Arrays.toString(args));
        return List.of();
    }
}
