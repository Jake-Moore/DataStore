package com.kamikazejam.datastore.base.serialization

import com.kamikazejam.datastore.base.Collection
import com.kamikazejam.datastore.store.Store
import kotlinx.serialization.KSerializer
import kotlin.reflect.KProperty

object SerializationUtil {
    fun getSerialName(serializer: KSerializer<*>, property: KProperty<*>): String {
        val propertyIndex = serializer.descriptor.getElementIndex(property.name)
        return serializer.descriptor.getElementName(propertyIndex)
    }

    fun <K : Any, X : Store<X, K>> getSerialName(collection: Collection<K, X>, property: KProperty<*>): String {
        return getSerialName(collection.getKSerializer(), property)
    }
}