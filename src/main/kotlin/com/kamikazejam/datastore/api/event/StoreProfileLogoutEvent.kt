package com.kamikazejam.datastore.api.event
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * This event is called when a player logs out, after their collections have been unloaded.<br></br>
 * Collection data should NOT be accessed in this event, use [StoreProfilePreLogoutEvent] instead.
 */
@Suppress("unused")
class StoreProfileLogoutEvent(val player: Player) : Event() {
    override fun getHandlers(): HandlerList {
        return handlerList
    }

    companion object {
        @JvmStatic
        val handlerList = HandlerList()
    }
}
