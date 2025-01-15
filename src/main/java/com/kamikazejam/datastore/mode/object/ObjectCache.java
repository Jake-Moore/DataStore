package com.kamikazejam.datastore.mode.object;

import com.kamikazejam.datastore.base.Cache;
import com.kamikazejam.datastore.base.result.StoreResult;
import com.mongodb.DuplicateKeyException;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NonBlocking;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Defines Object-specific getters for StoreObjects. They return non-null Optionals.
 */
@SuppressWarnings({"unused", "BlockingMethodInNonBlockingContext"})
public interface ObjectCache<X extends StoreObject<X>> extends Cache<String, X> {

    // ----------------------------------------------------- //
    //                 CRUD Helpers (Async)                  //
    // ----------------------------------------------------- //
    /**
     * Create a new Store object with the provided initializer.<br>
     * If you have a specific key for this Store, set it in the initializer.
     * @throws DuplicateKeyException If the key already exists in the cache. (failed to create)
     * @return The created Store object. (READ-ONLY)
     */
    @NonBlocking
    @NotNull
    default StoreResult<X> create(@NotNull Consumer<X> initializer) throws DuplicateKeyException {
        return StoreResult.of(CompletableFuture.supplyAsync(() -> createSync(initializer)), this);
    }

    // ----------------------------------------------------- //
    //                  CRUD Helpers (sync)                  //
    // ----------------------------------------------------- //
    /**
     * Create a new Store object with the provided initializer.<br>
     * If you have a specific key for this Store, set it in the initializer.
     * @throws DuplicateKeyException If the key already exists in the cache. (failed to create)
     * @return The created Store object. (READ-ONLY)
     */
    @Blocking
    @NotNull
    X createSync(@NotNull Consumer<X> initializer) throws DuplicateKeyException;
}
