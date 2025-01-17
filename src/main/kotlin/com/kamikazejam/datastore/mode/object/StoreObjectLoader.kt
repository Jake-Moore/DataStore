package com.kamikazejam.datastore.mode.`object`

import com.google.common.base.Preconditions
import com.kamikazejam.datastore.base.cache.StoreLoader
import java.lang.ref.WeakReference

class StoreObjectLoader<X : StoreObject<X>> internal constructor(cache: StoreObjectCollection<X>, identifier: String) :
    StoreLoader<X> {
    private val cache: StoreObjectCollection<X>
    private val identifier: String

    private var store: WeakReference<X>? = null
    private var loadedFromLocal = false

    init {
        Preconditions.checkNotNull(cache)
        Preconditions.checkNotNull(identifier)
        this.cache = cache
        this.identifier = identifier
    }

    @Suppress("SameParameterValue")
    private fun load(fromLocal: Boolean) {
        if (fromLocal) {
            val local: X? = cache.localStore[identifier]
            if (local != null) {
                // Ensure our Store is valid (not recently deleted)
                if (local.valid) {
                    this.store = WeakReference(local)
                    loadedFromLocal = true
                    return
                }

                // Nullify the reference if the Store is invalid
                // Don't quit, we could in theory still pull from the database
                cache.localStore.remove(identifier)
                this.store = WeakReference(null)
            }
        }

        val db = cache.readFromDatabase(identifier, true)
        db?.let { x ->
            store = WeakReference(x)
            loadedFromLocal = false
        }
    }

    override fun fetch(saveToLocalCache: Boolean): X? {
        load(true)

        // Double check validity here too
        store?.let { ref ->
            val p = ref.get()
            if (p != null && !p.valid) {
                store = WeakReference(null)
                return null
            }

            // Save to local cache if necessary
            if (saveToLocalCache && p != null && !loadedFromLocal) {
                cache.cache(p)
            }

            // Ensure the Store has its cache set
            p?.setCache(cache)
            return p
        }

        return null
    }
}
