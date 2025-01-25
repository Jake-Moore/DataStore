package com.kamikazejam.datastore.mode.`object`

import com.fasterxml.jackson.annotation.JsonIgnore
import com.google.common.base.Preconditions
import com.kamikazejam.datastore.base.Collection
import com.kamikazejam.datastore.base.Store
import com.kamikazejam.datastore.base.async.handler.crud.AsyncDeleteHandler
import com.kamikazejam.datastore.base.async.handler.crud.AsyncUpdateHandler
import com.kamikazejam.datastore.base.data.impl.bson.StoreDataLong
import com.kamikazejam.datastore.base.data.impl.bson.StoreDataString
import com.kamikazejam.datastore.base.field.FieldProvider
import com.kamikazejam.datastore.base.field.RequiredField
import com.kamikazejam.datastore.util.JacksonUtil.ID_FIELD
import com.kamikazejam.datastore.util.JacksonUtil.VERSION_FIELD
import org.jetbrains.annotations.ApiStatus
import java.util.*
import java.util.function.Consumer
import javax.persistence.Id
import kotlin.collections.ArrayList

@Suppress("unused")
abstract class StoreObject<X : StoreObject<X>> private constructor(
    @field:Transient @field:JsonIgnore override var readOnly: Boolean
) : Store<X, String> {
    // ----------------------------------------------------- //
    //                        Fields                         //
    // ----------------------------------------------------- //
    // The id of this object (as a user-defined String)
    @Id
    override val idField: RequiredField<StoreDataString> = RequiredField.of(ID_FIELD, StoreDataString(UUID.randomUUID().toString()))
    override val versionField: RequiredField<StoreDataLong> = RequiredField.of(VERSION_FIELD, StoreDataLong(0L))


    // ----------------------------------------------------- //
    //                      Transients                       //
    // ----------------------------------------------------- //
    @JsonIgnore
    @Transient
    private var collection: StoreObjectCollection<X>? = null

    @JsonIgnore
    @Transient
    override var valid: Boolean = true
        protected set

    @JsonIgnore
    @Transient
    protected var initialized: Boolean = false


    // ----------------------------------------------------- //
    //                     Constructors                      //
    // ----------------------------------------------------- //
    // For Jackson
    protected constructor() : this(true)

    // ----------------------------------------------------- //
    //                     CRUD Helpers                      //
    // ----------------------------------------------------- //
    override fun update(updateFunction: Consumer<X>): AsyncUpdateHandler<String, X> {
        return getCollection().update(this.id, updateFunction)
    }

    override fun delete(): AsyncDeleteHandler {
        return getCollection().delete(this.id)
    }

    // ----------------------------------------------------- //
    //                        Methods                        //
    // ----------------------------------------------------- //
    @ApiStatus.Internal
    override fun initialize() {
        if (initialized) {
            return
        }
        initialized = true // Must set before calling getAllFields because it will want it to be true
        // Set parent reference for all fields (including id and version)
        allFields.forEach { provider: FieldProvider ->
            provider.fieldWrapper.parent = this
        }
    }

    private fun ensureValid() {
        check(initialized) { "Document not initialized. Call initialize() after construction." }
        this.validateDuplicateFields() // may throw error
    }

    override fun getCollection(): Collection<String, X> {
        return collection ?: throw IllegalStateException("Collection is not set")
    }

    @get:ApiStatus.Internal
    override val allFields: List<FieldProvider>
        get() {
            this.ensureValid()
            val fields: MutableList<FieldProvider> = ArrayList(getCustomFields())
            fields.add(idField)
            fields.add(versionField)
            return fields
        }

    private fun validateDuplicateFields() {
        val names: MutableSet<String> = HashSet()
        names.add(idField.name)
        names.add(versionField.name)
        for (provider in getCustomFields()) {
            check(names.add(provider.fieldWrapper.name)) { "Duplicate field name: " + provider.fieldWrapper.name }
        }
    }

    @Suppress("DuplicatedCode")
    @get:ApiStatus.Internal
    override val allFieldsMap: Map<String, FieldProvider>
        get() {
            val map: MutableMap<String, FieldProvider> =
                HashMap()
            for (provider in allFields) {
                check(
                    !map.containsKey(
                        provider.fieldWrapper.name
                    )
                ) { "Duplicate field name: " + provider.fieldWrapper.name }
                map[provider.fieldWrapper.name] = provider
            }
            return map
        }

    override fun setCollection(collection: Collection<String, X>) {
        Preconditions.checkNotNull(collection, "Collection cannot be null")
        require(collection is StoreObjectCollection<X>) { "Collection must be a StoreObjectCollection" }
        this.collection = collection
    }

    override fun hashCode(): Int {
        return Objects.hashCode(this.id)
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }
        if (other !is StoreObject<*>) {
            return false
        }
        return this.idField == other.idField
    }

    override fun invalidate() {
        this.valid = false
    }
}
