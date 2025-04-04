package com.kamikazejam.datastore.api.event

import com.kamikazejam.datastore.store.StoreProfile
import com.kamikazejam.datastore.store.profile.StoreProfileCollection
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * This event is called when a player's StoreProfile is being removed from the collection.
 * Collection data can be accessed in this event, using the collection field.
 */
@Suppress("unused")
class StoreProfileQuitEvent<X : StoreProfile<X>>(
    val player: Player,
    val collection: StoreProfileCollection<X>,
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
