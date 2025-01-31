package com.kamikazejam.datastore.store

import com.google.common.base.Preconditions
import com.kamikazejam.datastore.base.Collection
import com.kamikazejam.datastore.base.async.handler.crud.AsyncDeleteHandler
import com.kamikazejam.datastore.base.async.handler.crud.AsyncUpdateHandler
import com.kamikazejam.datastore.base.serialization.SerializationUtil.ID_FIELD
import com.kamikazejam.datastore.base.serialization.SerializationUtil.VERSION_FIELD
import com.kamikazejam.datastore.store.`object`.StoreObjectCollection
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Objects
import java.util.UUID

@Suppress("unused")
@Serializable
abstract class StoreObject<X : StoreObject<X>>: Store<X, String> {
    // Pass these up to the StoreObject implementation
    @SerialName(ID_FIELD) override val id: String = UUID.randomUUID().toString()
    @SerialName(VERSION_FIELD) override val version: Long = 0L

    // ----------------------------------------------------- //
    //                     CRUD Helpers                      //
    // ----------------------------------------------------- //
    override fun update(updateFunction: (X) -> X): AsyncUpdateHandler<String, X> {
        return getCollection().update(this.id, updateFunction)
    }

    override fun delete(): AsyncDeleteHandler {
        return getCollection().delete(this.id)
    }



    // ----------------------------------------------------- //
    //                       API Methods                     //
    // ----------------------------------------------------- //

    @kotlinx.serialization.Transient @Transient
    override var valid: Boolean = true
        protected set

    @kotlinx.serialization.Transient @Transient
    private var collection: StoreObjectCollection<X>? = null

    override fun getCollection(): Collection<String, X> {
        return collection ?: throw IllegalStateException("Collection is not set")
    }



    // ----------------------------------------------------- //
    //                   Internal Methods                    //
    // ----------------------------------------------------- //

    override fun initialize(collection: Collection<String, X>) {
        if (this.collection == null) {
            Preconditions.checkNotNull(collection, "Collection cannot be null")
            require(collection is StoreObjectCollection<X>) { "Collection must be a StoreObjectCollection" }
            this.collection = collection
        }
    }

    override fun hashCode(): Int {
        return Objects.hashCode(this.id)
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is StoreObject<*>) return false
        return this.id == other.id
    }

    override fun invalidate() {
        this.valid = false
    }
}
