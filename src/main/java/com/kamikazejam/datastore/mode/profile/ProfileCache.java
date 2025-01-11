package com.kamikazejam.datastore.mode.profile;

import com.kamikazejam.datastore.base.Cache;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NonBlocking;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Defines Profile-specific getters for StoreObjects. They return non-null Optionals.
 * All get options (when not handshaking) will create if necessary. This is because every
 * Player UUID is assumed to have a StoreProfile
 */
@SuppressWarnings({"unused", "BlockingMethodInNonBlockingContext"})
public interface ProfileCache<X extends StoreProfile<X>> extends Cache<UUID, X> {

    // ----------------------------------------------------- //
    //                 CRUD Helpers (Async)                  //
    // ----------------------------------------------------- //

    /**
     * Read a StoreProfile (by player) from this cache (will fetch from database, will create if necessary)
     * @param player The player owning this store.
     * @return The StoreProfile object. (READ-ONLY)
     */
    @NonBlocking
    @NotNull
    default CompletableFuture<X> read(@NotNull Player player) {
        return this.read(player, true);
    }

    /**
     * Read a StoreProfile (by player) from this cache (will fetch from database, will create if necessary)
     * @param player The player owning this store.
     * @param cacheStore If we should cache the Store upon retrieval. (if it was found)
     * @return The StoreProfile object. (READ-ONLY)
     */
    @NonBlocking
    @NotNull
    default CompletableFuture<X> read(@NotNull Player player, boolean cacheStore) {
        return CompletableFuture.supplyAsync(() -> this.readSync(player, cacheStore));
    }

    /**
     * Modifies a Store in a controlled environment where modifications are allowed
     * @throws NoSuchElementException if the Store (by this key) is not found
     * @return The updated Store object. (READ-ONLY)
     */
    @NonBlocking
    default CompletableFuture<X> update(@NotNull Player player, @NotNull Consumer<X> updateFunction) {
        return CompletableFuture.supplyAsync(() -> this.updateSync(player, updateFunction));
    }

    /**
     * Deletes a Store (removes from both cache and database)
     */
    @NonBlocking
    default void delete(@NotNull Player player) {
        CompletableFuture.runAsync(() -> this.deleteSync(player));
    }


    // ----------------------------------------------------- //
    //                  CRUD Helpers (sync)                  //
    // ----------------------------------------------------- //
    /**
     * Read a StoreProfile (by player) from this cache (will fetch from database, will create if necessary)
     * @param player The player owning this store.
     * @return The StoreProfile object. (READ-ONLY)
     */
    @Blocking
    @NotNull
    default X readSync(@NotNull Player player) {
        return this.readSync(player, true);
    }

    /**
     * Read a StoreProfile (by player) from this cache (will fetch from database, will create if necessary)
     * @param player The player owning this store.
     * @param cacheStore If we should cache the Store upon retrieval. (if it was found)
     * @return The StoreProfile object. (READ-ONLY)
     */
    @Blocking
    @NotNull
    X readSync(@NotNull Player player, boolean cacheStore);

    /**
     * Modifies a Store in a controlled environment where modifications are allowed
     * @throws NoSuchElementException if the Store (by this key) is not found
     * @return The updated Store object. (READ-ONLY)
     */
    @Blocking
    default X updateSync(@NotNull Player player, @NotNull Consumer<X> updateFunction) {
        return this.updateSync(player.getUniqueId(), updateFunction);
    }

    /**
     * Deletes a Store (removes from both cache and database)
     */
    @Blocking
    default void deleteSync(@NotNull Player player) {
        this.deleteSync(player.getUniqueId());
    }

    // ------------------------------------------------------ //
    // Cache Methods                                          //
    // ------------------------------------------------------ //
    /**
     * Retrieve a Store from this cache (by player).
     * This method does NOT query the database.
     * @return The Store if it was cached.
     */
    @NonBlocking
    Optional<X> getFromCache(@NotNull Player player);

    /**
     * Retrieve a Store from the database (by player).
     * This method force queries the database, and updates this cache.
     * @param cacheStore If we should cache the Store upon retrieval. (if it was found)
     * @return The Store if it was found in the database.
     */
    @Blocking
    Optional<X> getFromDatabase(@NotNull Player player, boolean cacheStore);

    /**
     * Gets all online players' Profile objects. These should all be in the cache.
     */
    @NotNull
    Collection<X> getOnline();

    @ApiStatus.Internal
    void removeLoader(@NotNull UUID uuid);

    void onProfileLeaving(@NotNull Player player, @NotNull X profile);
}
