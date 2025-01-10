package com.kamikazejam.datastore.base.field;

import org.jetbrains.annotations.NotNull;

/**
 * Represents any object that can provide a FieldWrapper.
 * This is used to allow both FieldWrapper instances and wrapper collection classes
 * to be used in Store.getCustomFields().
 */
public interface FieldProvider {
    /**
     * Gets the underlying FieldWrapper that this object provides or represents.
     * @return The FieldWrapper instance
     */
    @NotNull FieldWrapper<?> getFieldWrapper();
}