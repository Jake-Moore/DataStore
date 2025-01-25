package com.kamikazejam.datastore.base

import com.kamikazejam.datastore.base.async.handler.crud.AsyncDeleteHandler
import com.kamikazejam.datastore.base.async.handler.crud.AsyncUpdateHandler
import com.kamikazejam.datastore.base.coroutine.DataStoreScope
import com.kamikazejam.datastore.base.data.SimpleStoreData
import com.kamikazejam.datastore.base.data.impl.StoreDataLong
import com.kamikazejam.datastore.base.field.FieldProvider
import com.kamikazejam.datastore.base.field.FieldWrapper
import com.kamikazejam.datastore.base.field.OptionalField
import com.kamikazejam.datastore.base.field.RequiredField
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.NonBlocking
import java.util.function.Consumer

/**
 * A Store is an object that can have CRUD operations performed on it.
 * Generics: K = Identifier Object Type (i.e. String, UUID)
 */
@Suppress("unused")
interface Store<X : Store<X, K>, K : Any> : DataStoreScope {

    // ----------------------------------------------------- //
    //                  User Defined Methods                 //
    // ----------------------------------------------------- //
    /**
     * Get all unique fields the Store object should serialize into its json data.
     *
     * You may return simple [FieldWrapper] objects like [RequiredField] or [OptionalField].
     */
    fun getCustomFields(): Set<FieldProvider>

    // ----------------------------------------------------- //
    //                 CRUD Helpers (Async)                  //
    // ----------------------------------------------------- //
    /**
     * Modifies this Store in a controlled environment where modifications are allowed
     * @return The updated Store object. (READ-ONLY)
     */
    @NonBlocking
    fun update(updateFunction: Consumer<X>): AsyncUpdateHandler<K, X> {
        return getCollection().update(id, updateFunction)
    }

    /**
     * Deletes this Store object (removes from both cache and database)
     */
    @NonBlocking
    fun delete(): AsyncDeleteHandler

    // ----------------------------------------------------- //
    //                Api / Internal Methods                 //
    // ----------------------------------------------------- //
    @ApiStatus.Internal
    fun initialize()

    @get:ApiStatus.Internal
    val allFields: Set<FieldProvider>

    @get:ApiStatus.Internal
    val allFieldsMap: Map<String, FieldProvider>

    /**
     * Gets the unique identifier of our Store. This can be a String representation of anything (like a UUID).
     * It just needs to be unique and able to be used as a key in a HashMap.
     *
     * @return K Identifier
     */
    val id: K
        get() = idField.getData()?.get() ?: throw IllegalStateException("idField is null")

    /**
     * Gets the Collection associated with this Store object.
     * Every Store has its Collection stored (non-persistent / transient) for easy access.
     *
     * @return Collection
     */
    fun getCollection(): Collection<K, X>

    /**
     * Sets the Collection associated with this Store object.
     */
    fun setCollection(collection: Collection<K, X>)

    /**
     * Gets the optimistic versioning FieldWrapper
     */
    val versionField: RequiredField<Long, StoreDataLong>

    /**
     * Gets the id FieldWrapper
     */
    val idField: FieldWrapper<K, out SimpleStoreData<K>>

    /**
     * @return If this Store is read-only right now
     */
    @set:ApiStatus.Internal
    var readOnly: Boolean

    /**
     * @return If this Store is valid and can be saved / updated. (i.e. not deleted)
     */
    val valid: Boolean

    /**
     * Makes this Store object invalid. It can no longer receive updates or be saved. (i.e. it was deleted)
     */
    fun invalidate()
}
