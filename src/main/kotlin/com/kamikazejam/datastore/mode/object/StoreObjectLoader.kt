package com.kamikazejam.datastore.mode.`object`

import com.google.common.base.Preconditions
import com.kamikazejam.datastore.base.loader.StoreLoader
import com.kamikazejam.datastore.mode.store.StoreObject
import java.lang.ref.WeakReference

class StoreObjectLoader<X : StoreObject<X>> internal constructor(collection: StoreObjectCollection<X>, identifier: String) : StoreLoader<X> {
    private val collection: StoreObjectCollection<X>
    private val identifier: String

    private var store: WeakReference<X>? = null
    private var loadedFromLocal = false

    init {
        Preconditions.checkNotNull(collection)
        Preconditions.checkNotNull(identifier)
        this.collection = collection
        this.identifier = identifier
    }

    @Suppress("SameParameterValue")
    private suspend fun load(fromLocal: Boolean) {
        if (fromLocal) {
            val local: X? = collection.localStore.get(identifier)
            if (local != null) {
                // Ensure our Store is valid (not recently deleted)
                if (local.valid) {
                    this.store = WeakReference(local)
                    loadedFromLocal = true
                    return
                }

                // Nullify the reference if the Store is invalid
                // Don't quit, we could in theory still pull from the database
                collection.localStore.remove(identifier)
                this.store = WeakReference(null)
            }
        }

        val db = collection.readFromDatabase(identifier, true)
        db?.let { x ->
            store = WeakReference(x)
            loadedFromLocal = false
        }
    }

    override suspend fun fetch(saveToLocalCache: Boolean): X? {
        load(true)

        // Double check validity here too
        store?.let { ref ->
            val p = ref.get()
            if (p != null && !p.valid) {
                store = WeakReference(null)
                return null
            }

            // Save to local if necessary
            if (saveToLocalCache && p != null && !loadedFromLocal) {
                collection.cache(p)
            }

            // Ensure the Store has its collection set
            p?.initialize(collection)
            return p
        }

        return null
    }
}
