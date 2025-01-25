package com.kamikazejam.datastore.base.field

import com.kamikazejam.datastore.base.Store

/**
 * Represents any object that can provide a [FieldWrapper].
 *
 * This is used to allow all implementations like [OptionalField], [RequiredField], etc.
 * to be returned in [Store.getCustomFields]
 */
interface FieldProvider {
    /**
     * Gets the underlying FieldWrapper that this object provides or represents.
     * @return The FieldWrapper instance
     */
    val fieldWrapper: FieldWrapper<*,*>
}