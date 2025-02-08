package com.kamikazejam.datastore.command.sub

import com.kamikazejam.datastore.DataStoreAPI
import com.kamikazejam.datastore.DataStoreSource
import com.kamikazejam.datastore.command.SubCommand
import com.kamikazejam.datastore.base.database.DatabaseRegistration
import com.kamikazejam.datastore.util.Color
import org.bukkit.command.CommandSender
import java.util.Objects

class CmdDatabases : SubCommand() {
    override val name: String
        get() = "databases"

    override val permission: String
        get() = "datastore.command.databases"

    override val argsDescription: String
        get() = ""

    override fun execute(sender: CommandSender, args: Array<String>) {
        sender.sendMessage(Color.t("&7***** &6Store Database &7*****"))
        val store = DataStoreSource.storageService
        sender.sendMessage(Color.t("&7Storage Service: " + (if (store.canWriteToStorage()) "&aConnected" else "&cDisconnected")))
        sender.sendMessage(Color.t("&7Databases:"))
        DataStoreAPI.databases.values.stream()
            .filter { obj: DatabaseRegistration? -> Objects.nonNull(obj) }
            .map { obj: DatabaseRegistration -> obj.databaseName }
            .forEach { n: String -> sender.sendMessage(Color.t("&7 - $n")) }
    }
}
