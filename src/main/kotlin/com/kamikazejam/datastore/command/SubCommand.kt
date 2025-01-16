package com.kamikazejam.datastore.command

import com.kamikazejam.datastore.util.Color
import org.bukkit.command.CommandSender

abstract class SubCommand {
    abstract val permission: String?
    abstract val name: String
    abstract val argsDescription: String

    fun sendNoPerm(sender: CommandSender) {
        sender.sendMessage(Color.t("&cYou do not have permission to use this command."))
    }

    fun sendUsage(sender: CommandSender) {
        sender.sendMessage(Color.t("&cNot enough command input. &eYou should use it like this:"))
        sender.sendMessage(Color.t("&b/datastore " + this.name + "&3 " + this.argsDescription))
    }

    /**
     * @param args The args of this subcommand, not including this command's name
     */
    abstract fun execute(sender: CommandSender, args: Array<String>)

    /**
     * @param args The args of this subcommand, not including this command's name
     */
    open fun getTabCompletions(sender: CommandSender, args: Array<String>): List<String> {
        return ArrayList()
    }
}
