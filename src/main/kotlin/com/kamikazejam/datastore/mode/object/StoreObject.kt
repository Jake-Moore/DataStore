package com.kamikazejam.datastore.mode.`object`

import com.fasterxml.jackson.annotation.JsonIgnore
import com.google.common.base.Preconditions
import com.kamikazejam.datastore.base.Collection
import com.kamikazejam.datastore.base.Store
import com.kamikazejam.datastore.base.field.FieldProvider
import com.kamikazejam.datastore.base.field.RequiredField
import com.kamikazejam.datastore.base.result.AsyncHandler
import org.jetbrains.annotations.ApiStatus
import java.util.*
import java.util.function.Consumer
import javax.persistence.Id

@Suppress("unused")
abstract class StoreObject<T : StoreObject<T>> private constructor(
    @field:Transient @field:JsonIgnore override var readOnly: Boolean
) : Store<T, String> {
    // ----------------------------------------------------- //
    //                        Fields                         //
    // ----------------------------------------------------- //
    // The id of this object (as a user-defined String)
    @Id
    override val idField: RequiredField<String> = RequiredField.of(
        "_id", UUID.randomUUID().toString(),
        String::class.java
    )
    override val versionField: RequiredField<Long> = RequiredField.of("version", 0L, Long::class.java)


    // ----------------------------------------------------- //
    //                      Transients                       //
    // ----------------------------------------------------- //
    @JsonIgnore
    @Transient
    private var collection: StoreObjectCollection<T>? = null

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
    override fun update(updateFunction: Consumer<T>): AsyncHandler<T> {
        return getCollection().update(this.id, updateFunction)
    }

    override fun delete(): AsyncHandler<Boolean> {
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
            provider.fieldWrapper.setParent(this)
        }
    }

    private fun ensureValid() {
        check(initialized) { "Document not initialized. Call initialize() after construction." }
        this.validateDuplicateFields() // may throw error
    }

    override fun getCollection(): Collection<String, T> {
        return collection ?: throw IllegalStateException("Collection is not set")
    }

    @get:ApiStatus.Internal
    override val allFields: Set<FieldProvider>
        get() {
            this.ensureValid()
            val fields: MutableSet<FieldProvider> = HashSet(getCustomFields())
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

    override fun setCollection(collection: Collection<String, T>) {
        Preconditions.checkNotNull(collection, "Collection cannot be null")
        require(collection is StoreObjectCollection<T>) { "Collection must be a StoreObjectCollection" }
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
