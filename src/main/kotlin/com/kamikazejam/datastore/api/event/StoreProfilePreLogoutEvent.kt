package com.kamikazejam.datastore.api.event
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * This event is called when a player logs out, just before their collections are going to be unloaded.<br></br>
 */
@Suppress("unused")
class StoreProfilePreLogoutEvent(val player: Player) : Event() {
    override fun getHandlers(): HandlerList {
        return handlerList
    }

    companion object {
        @JvmStatic
        val handlerList = HandlerList()
    }
}
