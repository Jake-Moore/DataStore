package com.kamikazejam.datastore.event.profile
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * This event is called when a player logs out, after their caches have been unloaded.<br></br>
 * Cache data should NOT be accessed in this event, use [StoreProfilePreLogoutEvent] instead.
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
