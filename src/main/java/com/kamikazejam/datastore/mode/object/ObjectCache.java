package com.kamikazejam.datastore.mode.object;

import com.kamikazejam.datastore.base.Cache;
import com.mongodb.DuplicateKeyException;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Defines Object-specific getters for StoreObjects. They return non-null Optionals.
 */
@SuppressWarnings("unused")
public interface ObjectCache<X extends StoreObject<X>> extends Cache<String, X> {

    // ------------------------------------------------------ //
    // CRUD Methods                                           //
    // ------------------------------------------------------ //

    // ------------------------------------------------------ //
    // CRUD Methods                                           //
    // ------------------------------------------------------ //
    /**
     * Create a new Store object with the provided key & initializer.<br>
     * If you have a specific key for this Store, set it in the initializer.
     * @throws DuplicateKeyException If the key already exists in the cache. (failed to create)
     * @return The created Store object. (READ-ONLY)
     */
    X create(@NotNull String key, @NotNull Consumer<X> initializer) throws DuplicateKeyException;

    /**
     * Create a new Store object with the provided initializer.<br>
     * If you have a specific key for this Store, set it in the initializer.
     * @throws DuplicateKeyException If the key already exists in the cache. (failed to create)
     * @return The created Store object. (READ-ONLY)
     */
    @NotNull
    X create(@NotNull Consumer<X> initializer) throws DuplicateKeyException;

    /**
     * Get a Store object from the cache or create a new one if it doesn't exist.<br>
     * This specific method will override any key set in the initializer. Since the key is an argument.
     * @param key The key of the Store to get or create.
     * @param initializer The initializer for the Store if it doesn't exist.
     * @return The Store object. (READ-ONLY) (fetched or created)
     */
    @NotNull
    X readOrCreate(@NotNull String key, @NotNull Consumer<X> initializer);
}
