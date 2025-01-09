package com.kamikazejam.datastore.util;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class for helping with players
 * You can give items (which drop near them if they are full), and clean their inventory (with or without armor)
 */
@SuppressWarnings("unused")
public class PlayerUtil {
    /**
     * @return true IFF (player != null AND player.isOnline() AND player.isValid())
     */
    public static boolean isFullyValidPlayer(@Nullable Player player) {
        return player != null && player.isOnline() && player.isValid();
    }
}
