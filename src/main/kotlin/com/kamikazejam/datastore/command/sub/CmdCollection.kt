package com.kamikazejam.datastore.command.sub

import com.kamikazejam.datastore.DataStoreAPI
import com.kamikazejam.datastore.base.Collection
import com.kamikazejam.datastore.base.Store
import com.kamikazejam.datastore.command.SubCommand
import com.kamikazejam.datastore.util.Color
import org.bukkit.command.CommandSender
import java.util.*

class CmdCollection : SubCommand() {
    override val name: String
        get() = "collection"
    override val permission: String
        get() = "datastore.command.collection"
    override val argsDescription: String
        get() = "<collection>"

    override fun execute(sender: CommandSender, args: Array<String>) {
        if (args.isEmpty()) {
            this.sendUsage(sender)
            return
        }

        val collName = args[0]
        val cache = DataStoreAPI.getCollection(collName)
        if (cache == null) {
            sender.sendMessage(Color.t("&cNo Collection matching \"$collName\". See `/datastore collections` for a list of them."))
            return
        }

        // Perform Command now that we have args
        val localCacheKeys = getSomeKeyStrings(cache)
        sender.sendMessage(Color.t("&7***** &6Store Cache: " + cache.name + " &7*****"))
        sender.sendMessage(
            Color.t(
                "&7" + cache.localCacheSize + " objects in local cache, first 10: " + localCacheKeys.toTypedArray()
                    .contentToString()
            )
        )
        sender.sendMessage(Color.t("&7Current State: " + (if (cache.running) "&aRunning" else "&cNot running")))
    }

    override fun getTabCompletions(sender: CommandSender, args: Array<String>): List<String> {
        val s = if (args.isEmpty()) "" else args[args.size - 1]
        return DataStoreAPI.collections.keys
            .stream()
            .filter { id: String -> id.lowercase(Locale.getDefault()).startsWith(s.lowercase(Locale.getDefault())) }
            .limit(20)
            .toList()
    }

    private fun <K, T : Store<T, K>> getSomeKeyStrings(c: Collection<K, T>): List<String> {
        return c.localStore.getKeyStrings(c).stream()
            .limit(10)
            .toList()
    }
}
