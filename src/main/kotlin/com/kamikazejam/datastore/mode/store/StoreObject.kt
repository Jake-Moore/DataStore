package com.kamikazejam.datastore.mode.store

import com.google.common.base.Preconditions
import com.kamikazejam.datastore.base.Collection
import com.kamikazejam.datastore.base.async.handler.crud.AsyncDeleteHandler
import com.kamikazejam.datastore.base.async.handler.crud.AsyncUpdateHandler
import com.kamikazejam.datastore.mode.`object`.StoreObjectCollection
import kotlinx.serialization.Serializable
import java.util.Objects

@Suppress("unused")
@Serializable
abstract class StoreObject<X : StoreObject<X>>(
    // Pass these up to the StoreObject implementation
    override val id: String,
    override val version: Long
) : Store<X, String> {

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
