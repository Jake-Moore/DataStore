package com.kamikazejam.datastore.command

import com.kamikazejam.datastore.command.sub.CmdCollection
import com.kamikazejam.datastore.command.sub.CmdCollections
import com.kamikazejam.datastore.command.sub.CmdDatabases
import com.kamikazejam.datastore.command.sub.CmdInfo
import com.kamikazejam.datastore.util.Color
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import java.util.Arrays
import java.util.Locale
import java.util.stream.Collectors

class DataStoreCommand : TabExecutor {
    private val subCommands: MutableList<SubCommand> = ArrayList()

    init {
        subCommands.add(CmdCollection())
        subCommands.add(CmdCollections())
        subCommands.add(CmdDatabases())
        subCommands.add(CmdInfo())
    }

    override fun onCommand(sender: CommandSender, cmd: Command, label: String, args: Array<String>): Boolean {
        if (!sender.hasPermission("datastore.command")) {
            sender.sendMessage(Color.t("&cYou do not have permission to use this command."))
            return true
        }

        // Attempt to run a subcommand first
        if (args.isNotEmpty()) {
            val subCommandName = args[0]
            val subCommand = subCommands.stream()
                .filter { sub: SubCommand -> sub.name.equals(subCommandName, ignoreCase = true) }
                .findFirst().orElse(null)
            if (subCommand != null) {
                // Validate they have permission for this subcommand
                if (subCommand.permission != null && !sender.hasPermission(subCommand.permission)) {
                    subCommand.sendNoPerm(sender)
                    return true
                }

                // Provide args but without this subcommand
                subCommand.execute(sender, Arrays.copyOfRange(args, 1, args.size))
                return true
            }
        }

        // Send the Help Message
        val subWithAccess: MutableList<SubCommand> = ArrayList()
        for (sub in subCommands) {
            if (sub.permission == null || sender.hasPermission(sub.permission)) {
                subWithAccess.add(sub)
            }
        }
        if (subWithAccess.isEmpty()) {
            sender.sendMessage(Color.t("&cYou do not have permission to use this command."))
            return true
        }
        // Send the help menu
        sender.sendMessage(Color.t("&6___[&2Help for command \"datastore\"&6]___"))
        subWithAccess.forEach { sub: SubCommand -> sender.sendMessage(Color.t("&b/datastore " + sub.name + "&3 " + sub.argsDescription)) }
        return true
    }

    override fun onTabComplete(sender: CommandSender, cmd: Command, label: String, args: Array<String>): List<String> {
        val subCommandNames = subCommands.stream()
            .map { obj: SubCommand -> obj.name }
            .collect(Collectors.toSet())

        // return subcommand names (if no arg stem has started)
        if (args.isEmpty()) {
            return ArrayList(subCommandNames)
        }

        // We have an arg stem, provide completions based on this stem
        if (args.size == 1) {
            val subCommandStem = args[0]
            return subCommandNames.stream()
                .filter { subCommand: String ->
                    subCommand.lowercase(Locale.getDefault()).startsWith(subCommandStem.lowercase(Locale.getDefault()))
                }
                .collect(Collectors.toList())
        }

        // We have more than 2 arg provided, we should ask for tab completions on that subcommand
        val subCommandName = args[0]
        val subCommand = subCommands.stream()
            .filter { sub: SubCommand -> sub.name.equals(subCommandName, ignoreCase = true) }
            .findFirst().orElse(null)
        if (subCommand == null) {
            return listOf()
        }

        // We have 2+ args and a subcommand, ask the subcommand for tab completions
        return subCommand.getTabCompletions(sender, Arrays.copyOfRange(args, 1, args.size))
    }
}
