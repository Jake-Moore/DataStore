package com.kamikazejam.datastore.util

import com.kamikazejam.datastore.store.profile.ProfileCollection
import org.bukkit.entity.Player
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import java.util.UUID

/**
 * Data class to help provide the [ProfileCollection.defaultInitializer] with basic [Player] information on join (when auto creating a profile)
 */
data class PlayerJoinDetails(
    val uuid: UUID,
    val username: String,
    val event: AsyncPlayerPreLoginEvent,
)