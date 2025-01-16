package com.kamikazejam.datastore.base.field

import com.kamikazejam.datastore.base.Store

/**
 * Represents any object that can provide a [FieldWrapper].
 *
 * This is used to allow both plain [FieldWrapper] classes and wrapper classes (like [FieldWrapperMap])
 * to be returned in [Store.getCustomFields]
 */
interface FieldProvider {
    /**
     * Gets the underlying FieldWrapper that this object provides or represents.
     * @return The FieldWrapper instance
     */
    val fieldWrapper: FieldWrapper<*>
}