package com.kamikazejam.datastore.base.data

import com.kamikazejam.datastore.base.field.FieldProvider

/**
 * StoreData must contain a nonnull value, however at the field level, the field will return StoreData<T>?
 *
 * This allows null values, where internally they are handled as null StoreData, not a null internal value.
 */
interface StoreData<T : Any> {
    val dataType: Class<T>
    var value: T

    fun getCustomFields(): Set<FieldProvider>
}