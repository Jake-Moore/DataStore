package com.kamikazejam.datastore.event.profile
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * This event is called when a player logs out, just before their caches are going to be unloaded.<br></br>
 */
@Suppress("unused")
class StoreProfilePreLogoutEvent(private val player: Player) : Event() {
    override fun getHandlers(): HandlerList {
        return handlerList
    }

    companion object {
        val handlerList: HandlerList = HandlerList()
    }
}
