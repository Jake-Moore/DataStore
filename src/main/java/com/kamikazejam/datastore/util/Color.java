package com.kamikazejam.datastore.util;

import org.bukkit.ChatColor;
import org.jetbrains.annotations.Contract;

public class Color {
    @Contract("null -> null")
    public static String t(String msg) {
        if (msg == null) { return null; }
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}
