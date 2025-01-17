package com.kamikazejam.datastore.event.profile

import com.kamikazejam.datastore.mode.profile.StoreProfile
import com.kamikazejam.datastore.mode.profile.StoreProfileCollection
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * This event is called when a player's StoreProfile is being removed from the cache.
 * Cache data can be accessed in this event, using the cache field.
 */
@Suppress("unused")
class StoreProfileQuitEvent<X : StoreProfile<X>>(
    val player: Player,
    val cache: StoreProfileCollection<X>,
    val profile: X
) : Event() {
    override fun getHandlers(): HandlerList {
        return handlerList
    }

    companion object {
        @JvmStatic
        val handlerList = HandlerList()
    }
}
