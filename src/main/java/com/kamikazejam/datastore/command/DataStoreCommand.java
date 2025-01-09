package com.kamikazejam.datastore.command;

import com.kamikazejam.datastore.command.sub.CmdCache;
import com.kamikazejam.datastore.command.sub.CmdCaches;
import com.kamikazejam.datastore.command.sub.CmdDatabases;
import com.kamikazejam.datastore.command.sub.CmdInfo;
import com.kamikazejam.datastore.util.Color;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DataStoreCommand implements TabExecutor {
    private final List<SubCommand> subCommands = new ArrayList<>();
    public DataStoreCommand() {
        subCommands.add(new CmdCache());
        subCommands.add(new CmdCaches());
        subCommands.add(new CmdDatabases());
        subCommands.add(new CmdInfo());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("datastore.command")) {
            sender.sendMessage(Color.t("&cYou do not have permission to use this command."));
            return true;
        }

        // Attempt to run a subcommand first
        if (args.length >= 1) {
            String subCommandName = args[0];
            SubCommand subCommand = subCommands.stream()
                    .filter(sub -> sub.getName().equalsIgnoreCase(subCommandName))
                    .findFirst().orElse(null);
            if (subCommand != null) {
                // Validate they have permission for this subcommand
                if (subCommand.getPermission() != null && !sender.hasPermission(subCommand.getPermission())) {
                    subCommand.sendNoPerm(sender);
                    return true;
                }

                // Provide args but without this subcommand
                subCommand.execute(sender, Arrays.copyOfRange(args, 1, args.length));
                return true;
            }
        }

        // Send the Help Message
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
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        Set<String> subCommandNames = subCommands.stream()
                .map(SubCommand::getName)
                .collect(Collectors.toSet());

        // return subcommand names (if no arg stem has started)
        if (args.length == 0) {
            return new ArrayList<>(subCommandNames);
        }

        // We have an arg stem, provide completions based on this stem
        if (args.length == 1) {
            String subCommandStem = args[0];
            return subCommandNames.stream()
                    .filter(subCommand -> subCommand.toLowerCase().startsWith(subCommandStem.toLowerCase()))
                    .collect(Collectors.toList());
        }

        // We have more than 2 arg provided, we should ask for tab completions on that subcommand
        String subCommandName = args[0];
        SubCommand subCommand = subCommands.stream()
                .filter(sub -> sub.getName().equalsIgnoreCase(subCommandName))
                .findFirst().orElse(null);
        if (subCommand == null) { return List.of(); }

        // We have 2+ args and a subcommand, ask the subcommand for tab completions
        return subCommand.getTabCompletions(sender, Arrays.copyOfRange(args, 1, args.length));
    }
}
