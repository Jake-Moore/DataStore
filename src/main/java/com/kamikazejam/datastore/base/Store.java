package com.kamikazejam.datastore.base;

import com.kamikazejam.datastore.base.field.FieldProvider;
import com.kamikazejam.datastore.base.field.FieldWrapper;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NonBlocking;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * A Store is an object that can be cached, saved, or loaded within DataStore.
 * Generics: K = Identifier Object Type (i.e. String, UUID)
 */
@SuppressWarnings({"UnusedReturnValue", "unused"})
public interface Store<T extends Store<T, K>, K> {

    // ----------------------------------------------------- //
    //                  User Defined Methods                 //
    // ----------------------------------------------------- //
    /**
     * Get all unique fields the Store object should serialize into its json data.
     */
    @NotNull
    Set<FieldProvider> getCustomFields();

    // ----------------------------------------------------- //
    //                 CRUD Helpers (Async)                  //
    // ----------------------------------------------------- //
    /**
     * Modifies this Store in a controlled environment where modifications are allowed
     * @return The updated Store object. (READ-ONLY)
     */
    @NonBlocking
    CompletableFuture<T> update(@NotNull Consumer<T> updateFunction);

    /**
     * Deletes this Store object (removes from both cache and database)
     */
    @NonBlocking
    default void delete() {
        CompletableFuture.runAsync(this::deleteSync);
    }

    // ----------------------------------------------------- //
    //                  CRUD Helpers (sync)                  //
    // ----------------------------------------------------- //
    /**
     * Modifies this Store in a controlled environment where modifications are allowed
     * @return The updated Store object. (READ-ONLY)
     */
    @Blocking
    T updateSync(@NotNull Consumer<T> updateFunction);

    /**
     * Deletes this Store object (removes from both cache and database)
     */
    @Blocking
    void deleteSync();

    // ----------------------------------------------------- //
    //                Api / Internal Methods                 //
    // ----------------------------------------------------- //

    @ApiStatus.Internal
    void initialize();
    @ApiStatus.Internal
    void setReadOnly(boolean readOnly);
    @ApiStatus.Internal
    @NotNull Set<FieldProvider> getAllFields();
    @ApiStatus.Internal
    @NotNull Map<String, FieldProvider> getAllFieldsMap();

    /**
     * Gets the unique identifier of our Store. This can be a String representation of anything (like a UUID).
     * It just needs to be unique and able to be used as a key in a HashMap.
     *
     * @return K Identifier
     */
    @NotNull
    K getId();

    /**
     * Gets the cache associated with this Store object.
     * Every Store has its cache stored (non-persistent / transient) for easy access.
     *
     * @return Cache
     */
    @NotNull
    Cache<K, ?> getCache();

    /**
     * Sets the cache associated with this Store object.
     */
    void setCache(Cache<K, T> cache);

    /**
     * @return A hash code based on any identifying fields for this Store.
     */
    int hashCode();

    /**
     * Use identifying fields to determine if two Store objects are equal.
     *
     * @return if the two Store objects have matching identification
     */
    boolean equals(Object object);

    /**
     * Gets the optimistic versioning FieldWrapper
     */
    @NotNull
    FieldWrapper<Long> getVersionField();

    /**
     * Gets the id FieldWrapper
     */
    @NotNull
    FieldWrapper<K> getIdField();

    /**
     * @return If this Store is read-only right now
     */
    boolean isReadOnly();

    /**
     * @return If this Store is valid and can be saved / updated. (i.e. not deleted)
     */
    boolean isValid();

    /**
     * Makes this Store object invalid. It can no longer receive updates or be saved. (i.e. it was deleted)
     */
    void invalidate();
}
