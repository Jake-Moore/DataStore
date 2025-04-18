package com.kamikazejam.datastore.store

import com.kamikazejam.datastore.base.Collection
import com.kamikazejam.datastore.base.async.handler.crud.AsyncDeleteHandler
import com.kamikazejam.datastore.base.async.handler.crud.AsyncRejectableUpdateHandler
import com.kamikazejam.datastore.base.async.handler.crud.AsyncUpdateHandler
import com.kamikazejam.datastore.base.coroutine.DataStoreScope
import com.kamikazejam.datastore.base.exception.update.RejectUpdateException
import kotlinx.serialization.Serializable
import org.jetbrains.annotations.ApiStatus.Internal
import org.jetbrains.annotations.NonBlocking
import java.util.UUID

/**
 * A Store is an object that can have CRUD operations performed on it.
 * Generics: K = Identifier Object Type (i.e. String, UUID)
 */
@Suppress("unused")
@Serializable
sealed interface Store<X : Store<X, K>, K : Any> : DataStoreScope {

    // ----------------------------------------------------- //
    //                       Properties                      //
    // ----------------------------------------------------- //
    /**
     * The unique identifier of our [Store]. This is some [K] (like a [String] or [UUID]).
     * It just needs to be unique and able to be used as a key in a HashMap.
     */
    val id: K

    /**
     * The version of this [Store]. This is used for optimistic versioning & cas operations.
     */
    val version: Long


    // ----------------------------------------------------- //
    //                 CRUD Helpers (Async)                  //
    // ----------------------------------------------------- //
    /**
     * Modifies this Store in a controlled environment where modifications are allowed
     * @return The updated Store object. (READ-ONLY)
     */
    @NonBlocking
    fun update(updateFunction: (X) -> X): AsyncUpdateHandler<K, X> {
        return getCollection().update(id, updateFunction)
    }

    /**
     * Modifies a Store in a controlled environment where modifications are allowed
     * The updateFunction can choose to reject the update by throwing a [RejectUpdateException]
     * @return The updated Store object. (READ-ONLY)
     */
    @NonBlocking
    fun updateRejectable(updateFunction: (X) -> X): AsyncRejectableUpdateHandler<K, X> {
        return getCollection().updateRejectable(id, updateFunction)
    }

    /**
     * Deletes this Store object (removes from both cache and database)
     */
    @NonBlocking
    fun delete(): AsyncDeleteHandler


    // ----------------------------------------------------- //
    //                       API Methods                     //
    // ----------------------------------------------------- //
    /**
     * If this [Store] is valid and can be saved / updated. (i.e. not deleted)
     */
    val valid: Boolean

    /**
     * Gets the Collection associated with this Store object.
     * Every Store has its Collection stored (non-persistent / transient) for easy access.
     *
     * @return Collection
     */
    fun getCollection(): Collection<K, X>


    // ----------------------------------------------------- //
    //                   Data Class Methods                  //
    // ----------------------------------------------------- //
    fun copyHelper(version: Long): X

    // ----------------------------------------------------- //
    //                    Internal Methods                   //
    // ----------------------------------------------------- //
    @Internal
    fun initialize(collection: Collection<K, X>)

    /**
     * Makes this Store object invalid. It can no longer receive updates or be saved. (i.e. it was deleted)
     */
    @Internal
    fun invalidate() {}
}
