package com.kamikazejam.datastore.command.sub

import com.kamikazejam.datastore.DataStoreSource
import com.kamikazejam.datastore.command.SubCommand
import com.kamikazejam.datastore.util.Color
import org.bukkit.command.CommandSender

class CmdInfo : SubCommand() {
    override val name: String
        get() = "info"

    override val permission: String
        get() = "datastore.command.info"

    override val argsDescription: String
        get() = ""

    override fun execute(sender: CommandSender, args: Array<String>) {
        sender.sendMessage(Color.t("&7--- &6DataStore Information&7---"))
        sender.sendMessage(Color.t("&7Database Prefix:"))
        sender.sendMessage(Color.t("  &6" + DataStoreSource.storeDbPrefix))
        sender.sendMessage(Color.t("&7Storage Service:"))
        sender.sendMessage(Color.t("  &6Name: " + DataStoreSource.storageMode.name))
        val r = if (DataStoreSource.storageService.canCache()) "&aYes" else "&cNo"
        sender.sendMessage(Color.t("  &6Ready: $r"))
    }
}
