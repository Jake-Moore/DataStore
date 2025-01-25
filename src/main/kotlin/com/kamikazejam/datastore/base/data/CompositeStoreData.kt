package com.kamikazejam.datastore.base.data

import com.kamikazejam.datastore.base.field.FieldProvider

/**
 * Implementation of StoreData for complex types that can contain nested StoreData fields
 */
abstract class CompositeStoreData<T : Any>: StoreData<T>() {
    /**
     * Override this method to provide the nested fields this composite data contains
     */
    abstract fun getCustomFields(): Set<FieldProvider>

    final override fun getType(): Companion.Type {
        return Companion.Type.COMPOSITE
    }
}