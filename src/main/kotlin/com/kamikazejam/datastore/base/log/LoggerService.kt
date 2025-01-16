package com.kamikazejam.datastore.base.log

import com.kamikazejam.datastore.util.Color
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import java.util.logging.Level

@Suppress("unused", "MemberVisibilityCanBePrivate")
abstract class LoggerService {
    // Abstraction
    abstract val loggerName: String

    abstract val plugin: Plugin

    abstract val isDebug: Boolean

    // Public Methods
    fun info(msg: String) {
        logToConsole(msg, Level.INFO)
    }

    fun info(throwable: Throwable) {
        logToConsole(throwable.message, Level.INFO)
        throwable.printStackTrace()
    }

    fun info(throwable: Throwable, msg: String) {
        logToConsole(msg + " - " + throwable.message, Level.INFO)
        throwable.printStackTrace()
    }

    fun debug(msg: String) {
        if (!isDebug) {
            return
        }
        logToConsole(msg, Level.FINE)
    }

    fun warn(msg: String) {
        logToConsole(msg, Level.WARNING)
    }

    fun warn(throwable: Throwable) {
        logToConsole(throwable.message, Level.WARNING)
        throwable.printStackTrace()
    }

    fun warn(throwable: Throwable, msg: String) {
        logToConsole(msg + " - " + throwable.message, Level.WARNING)
        throwable.printStackTrace()
    }

    fun warning(msg: String) {
        this.warn(msg)
    }

    fun warning(throwable: Throwable) {
        this.warn(throwable)
    }

    fun warning(throwable: Throwable, msg: String) {
        this.warn(throwable, msg)
    }

    fun severe(msg: String) {
        logToConsole(msg, Level.SEVERE)
    }

    fun severe(throwable: Throwable) {
        logToConsole(throwable.message, Level.SEVERE)
        throwable.printStackTrace()
    }

    fun severe(throwable: Throwable, msg: String) {
        logToConsole(msg + " - " + throwable.message, Level.SEVERE)
        throwable.printStackTrace()
    }

    fun error(msg: String) {
        this.severe(msg)
    }

    fun error(throwable: Throwable) {
        this.severe(throwable)
    }

    fun error(throwable: Throwable, msg: String) {
        this.severe(throwable, msg)
    }

    fun logToConsole(msg: String?, level: Level) {
        val plPrefix = "[" + plugin.name + "] "
        if (level === Level.FINE) {
            Bukkit.getConsoleSender().sendMessage(Color.t("&7[DEBUG] $plPrefix$msg"))
        } else if (level === Level.INFO) {
            Bukkit.getConsoleSender().sendMessage(Color.t(plPrefix + msg))
        } else if (level === Level.WARNING) {
            Bukkit.getConsoleSender().sendMessage(Color.t("&e[WARNING] $plPrefix$msg"))
        } else if (level === Level.SEVERE) {
            Bukkit.getConsoleSender().sendMessage(Color.t("&c[SEVERE] $plPrefix$msg"))
        }
    }
}
