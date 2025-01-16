package com.kamikazejam.datastore.util

import org.bukkit.entity.Player

/**
 * Utility class for helping with players
 * You can give items (which drop near them if they are full), and clean their inventory (with or without armor)
 */
@Suppress("unused")
object PlayerUtil {
    /**
     * @return true IFF (player != null AND player.isOnline() AND player.isValid())
     */
    fun isFullyValidPlayer(player: Player?): Boolean {
        return player != null && player.isOnline && player.isValid
    }
}
