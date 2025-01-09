package com.kamikazejam.datastore.util;

import com.kamikazejam.datastore.DataStoreSource;
import com.kamikazejam.datastore.base.Cache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Utility class for logging stack traces to file
 * For developer warnings, developers need the trace, but don't necessarily need to spam the console
 * We can print helpful stack traces to a log file, and send a reduced warning to the console
 */
@SuppressWarnings({"UnusedReturnValue", "unused"})
public class DataStoreFileLogger {

    @Nullable
    public static File logToFile(@NotNull String msg, @NotNull Level level, @NotNull File file) {
        if (appendToFile(createStackTrace(msg), file)) {
            DataStoreSource.get().getColorLogger().logToConsole(msg + " (Logged to " + "/logs/" + file.getName() + ")", level);
            return file;
        }
        return null;
    }

    /**
     * Logs a warning message to the console, and saves the current stack trace to a log file
     * @return The file written, if successful
     */
    @Nullable
    public static File warn(@NotNull Cache<?,?> cache, @NotNull String msg) {
        return logToFile(msg, Level.WARNING, getFileByCache(cache));
    }

    /**
     * Logs a warning message to the console, and saves the current stack trace to a log file
     * Also appends the given trace to the file
     * @return The file written, if successful
     */
    @Nullable
    public static File warn(@NotNull Cache<?,?> cache, @NotNull String msg, @NotNull Throwable trace) {
        File file = logToFile(msg, Level.WARNING, getFileByCache(cache));
        if (file == null) { return null; }

        // Add some empty lines for separation
        if (!appendToFile(List.of("", "", "Extra Trace (if necessary)", ""), file)) {
            return null;
        }

        // Save the original trace after
        if (!appendToFile(trace, file)) {
            return null;
        }
        return file;
    }

    /**
     * Logs a warning message to the console, and saves the current stack trace to a log file
     * @return The file written, if successful
     */
    @Nullable
    public static File warn(@NotNull String msg) {
        return logToFile(msg, Level.WARNING, getRandomFile());
    }

    /**
     * Logs a warning message to the console, and saves the current stack trace to a log file
     * Also appends the given trace to the file
     * @return The file written, if successful
     */
    @Nullable
    public static File warn(@NotNull String msg, @NotNull Throwable trace) {
        File file = logToFile(msg, Level.WARNING, getRandomFile());
        if (file == null) { return null; }

        // Add some empty lines for separation
        if (!appendToFile(List.of("", "", "Extra Trace (if necessary)", ""), file)) {
            return null;
        }

        // Save the original trace after
        if (!appendToFile(trace, file)) {
            return null;
        }
        return file;
    }

    public static Throwable createStackTrace(@NotNull String msg) {
        try {
            throw new Exception(msg);
        }catch (Throwable t) {
            return t;
        }
    }

    public static boolean appendToFile(@NotNull Throwable throwable, @NotNull File file) {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            boolean ignored = parent.mkdirs();
        }

        try (FileWriter fileWriter = new FileWriter(file, true); PrintWriter printWriter = new PrintWriter(fileWriter)) {
            // Write the stack trace to the file
            throwable.printStackTrace(printWriter);
            return true;
        } catch (IOException e) {
            DataStoreSource.get().getColorLogger().severe("Failed to write stack trace to file (" + file.getAbsoluteFile() + "): " + e.getMessage());
            return false;
        }
    }

    public static boolean appendToFile(@NotNull List<String> lines, @NotNull File file) {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            boolean ignored = parent.mkdirs();
        }

        try (FileWriter fileWriter = new FileWriter(file, true); PrintWriter printWriter = new PrintWriter(fileWriter)) {
            // Write the stack trace to the file
            lines.forEach(printWriter::println);
            return true;
        } catch (IOException e) {
            DataStoreSource.get().getColorLogger().severe("Failed to write stack trace to file (" + file.getAbsoluteFile() + "): " + e.getMessage());
            return false;
        }
    }

    @NotNull
    private static File getFileByCache(@NotNull Cache<?, ?> cache) {
        // Print the message + a stack trace to a file
        String fileName = cache.getPlugin().getName() + "_" + cache.getName() + "_" + System.currentTimeMillis() + ".log";
        return new File(DataStoreSource.get().getDataFolder() + File.separator + "logs", fileName);
    }

    @NotNull
    private static File getRandomFile() {
        String fileName = "log_" + System.currentTimeMillis() + "_" + UUID.randomUUID() + ".log";
        return new File(DataStoreSource.get().getDataFolder() + File.separator + "logs", fileName);
    }
}
