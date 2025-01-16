package com.kamikazejam.datastore.command.sub

import com.kamikazejam.datastore.DataStoreAPI
import com.kamikazejam.datastore.command.SubCommand
import com.kamikazejam.datastore.util.Color
import org.bukkit.command.CommandSender

class CmdCaches : SubCommand() {
    override val name: String
        get() = "caches"

    override val permission: String
        get() = "datastore.command.caches"

    override val argsDescription: String
        get() = ""

    override fun execute(sender: CommandSender, args: Array<String>) {
        sender.sendMessage(Color.t("&7***** &6Store Caches &7*****"))
        for (c in DataStoreAPI.caches.values) {
            sender.sendMessage(Color.t("&7" + c.name + " - " + c.localCacheSize + " local objects"))
        }
    }
}
