package com.kamikazejam.datastore.util

import com.kamikazejam.datastore.DataStoreSource
import com.kamikazejam.datastore.base.Collection
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.PrintWriter
import java.util.*
import java.util.logging.Level

/**
 * Utility class for logging stack traces to file
 * For developer warnings, developers need the trace, but don't necessarily need to spam the console
 * We can print helpful stack traces to a log file, and send a reduced warning to the console
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
object DataStoreFileLogger {
    fun logToFile(msg: String, level: Level, file: File): File? {
        if (appendToFile(createStackTrace(msg), file)) {
            DataStoreSource.colorLogger.logToConsole(msg + " (Logged to " + "/logs/" + file.name + ")", level)
            return file
        }
        return null
    }

    /**
     * Logs a warning message to the console, and saves the current stack trace to a log file
     * @return The file written, if successful
     */
    fun warn(collection: Collection<*, *>, msg: String): File? {
        return logToFile(msg, Level.WARNING, getFileByCache(collection))
    }

    /**
     * Logs a warning message to the console, and saves the current stack trace to a log file
     * Also appends the given trace to the file
     * @return The file written, if successful
     */
    fun warn(collection: Collection<*, *>, msg: String, trace: Throwable): File? {
        val file = logToFile(
            msg,
            Level.WARNING,
            getFileByCache(collection)
        )
            ?: return null

        // Add some empty lines for separation
        if (!appendToFile(listOf("", "", "Extra Trace (if necessary)", ""), file)) {
            return null
        }

        // Save the original trace after
        if (!appendToFile(trace, file)) {
            return null
        }
        return file
    }

    /**
     * Logs a warning message to the console, and saves the current stack trace to a log file
     * @return The file written, if successful
     */
    fun warn(msg: String): File? {
        return logToFile(msg, Level.WARNING, randomFile)
    }

    /**
     * Logs a warning message to the console, and saves the current stack trace to a log file
     * Also appends the given trace to the file
     * @return The file written, if successful
     */
    fun warn(msg: String, trace: Throwable): File? {
        val file = logToFile(
            msg, Level.WARNING,
            randomFile
        )
        if (file == null) {
            return null
        }

        // Add some empty lines for separation
        if (!appendToFile(listOf("", "", "Extra Trace (if necessary)", ""), file)) {
            return null
        }

        // Save the original trace after
        if (!appendToFile(trace, file)) {
            return null
        }
        return file
    }

    fun createStackTrace(msg: String): Throwable {
        try {
            throw Exception(msg)
        } catch (t: Throwable) {
            return t
        }
    }

    fun appendToFile(throwable: Throwable, file: File): Boolean {
        val parent = file.parentFile
        if (parent != null && !parent.exists()) {
            parent.mkdirs()
        }

        try {
            FileWriter(file, true).use { fileWriter ->
                PrintWriter(fileWriter).use { printWriter ->
                    // Write the stack trace to the file
                    throwable.printStackTrace(printWriter)
                    return true
                }
            }
        } catch (e: IOException) {
            DataStoreSource.colorLogger.severe("Failed to write stack trace to file (" + file.absoluteFile + "): " + e.message)
            return false
        }
    }

    fun appendToFile(lines: List<String?>, file: File): Boolean {
        val parent = file.parentFile
        if (parent != null && !parent.exists()) {
            parent.mkdirs()
        }

        try {
            FileWriter(file, true).use { fileWriter ->
                PrintWriter(fileWriter).use { printWriter ->
                    // Write the stack trace to the file
                    lines.forEach { x: String? -> printWriter.println(x) }
                    return true
                }
            }
        } catch (e: IOException) {
            DataStoreSource.colorLogger.severe("Failed to write stack trace to file (" + file.absoluteFile + "): " + e.message)
            return false
        }
    }

    private fun getFileByCache(collection: Collection<*, *>): File {
        // Print the message + a stack trace to a file
        val fileName = collection.plugin.name + "_" + collection.name + "_" + System.currentTimeMillis() + ".log"
        return File(
            DataStoreSource.get().dataFolder.toString() + File.separator + "logs" + File.separator + "datastore",
            fileName
        )
    }

    private val randomFile: File
        get() {
            val fileName =
                "log_" + System.currentTimeMillis() + "_" + UUID.randomUUID() + ".log"
            return File(
                DataStoreSource.get().dataFolder
                    .toString() + File.separator + "logs" + File.separator + "datastore", fileName
            )
        }
}
