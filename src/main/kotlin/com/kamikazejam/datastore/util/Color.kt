package com.kamikazejam.datastore.util

import org.bukkit.ChatColor
import org.jetbrains.annotations.Contract

object Color {
    @Contract("null -> null")
    fun t(msg: String?): String? {
        if (msg == null) {
            return null
        }
        return ChatColor.translateAlternateColorCodes('&', msg)
    }
    fun tNotNull(msg: String): String {
        return ChatColor.translateAlternateColorCodes('&', msg)
    }
}
