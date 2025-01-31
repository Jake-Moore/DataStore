package com.kamikazejam.datastore.base.serialization

import com.kamikazejam.datastore.base.Collection
import com.kamikazejam.datastore.store.Store
import com.kamikazejam.datastore.store.StoreProfile
import com.kamikazejam.datastore.store.profile.ProfileCollection
import kotlinx.serialization.SerialName
import kotlin.reflect.KProperty
import kotlin.reflect.full.findAnnotation

@Suppress("unused")
object SerializationUtil {
    fun getSerialNameFromProperty(property: KProperty<*>): String {
        return property.findAnnotation<SerialName>()?.value ?: property.name
    }
    fun <K : Any, X : Store<X, K>> getSerialNameForID(collection: Collection<K, X>): String {
        return getSerialNameFromProperty(collection.getIdKProperty())
    }
    fun <K : Any, X : Store<X, K>> getSerialNameForVersion(collection: Collection<K, X>): String {
        return getSerialNameFromProperty(collection.getVersionKProperty())
    }
    fun <X : StoreProfile<X>> getSerialNameForUsername(collection: ProfileCollection<X>): String {
        return getSerialNameFromProperty(collection.getUsernameKProperty())
    }
}