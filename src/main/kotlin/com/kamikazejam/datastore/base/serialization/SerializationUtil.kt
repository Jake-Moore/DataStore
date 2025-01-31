package com.kamikazejam.datastore.base.serialization

import kotlinx.serialization.SerialName
import kotlin.reflect.KProperty
import kotlin.reflect.full.findAnnotation

object SerializationUtil {
    fun getSerialName(property: KProperty<*>): String {
        return property.findAnnotation<SerialName>()?.value ?: property.name
    }
}