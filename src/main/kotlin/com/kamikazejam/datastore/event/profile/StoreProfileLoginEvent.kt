package com.kamikazejam.datastore.event.profile
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * This event is called when a player logs in, after their caches have been loaded
 */
@Suppress("unused")
class StoreProfileLoginEvent(val player: Player) : Event() {
    override fun getHandlers(): HandlerList {
        return handlerList
    }

    companion object {
        @JvmStatic
        val handlerList = HandlerList()
    }
}
