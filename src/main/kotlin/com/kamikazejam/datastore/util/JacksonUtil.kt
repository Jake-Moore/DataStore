package com.kamikazejam.datastore.util

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.introspect.VisibilityChecker
import com.google.common.base.Preconditions
import com.kamikazejam.datastore.DataStoreSource
import com.kamikazejam.datastore.base.Store
import com.kamikazejam.datastore.base.field.FieldProvider
import com.kamikazejam.datastore.base.field.FieldWrapper
import com.kamikazejam.datastore.base.field.OptionalField
import com.kamikazejam.datastore.base.field.RequiredField
import com.kamikazejam.datastore.util.jackson.JacksonSpigotModule
import org.bson.Document

@Suppress("unused")
object JacksonUtil {
    const val ID_FIELD: String = "_id"
    const val VERSION_FIELD: String = "version"

    private var _objectMapper: ObjectMapper? = null

    fun loadObjectMapper() {
        objectMapper
    }

    fun serializeValue(value: Any): String {
        return objectMapper.writeValueAsString(value)
    }

    fun <T> deserializeValue(json: String, clazz: Class<T>): T {
        return objectMapper.readValue(json, clazz)
    }

    private val objectMapper: ObjectMapper
        get() {
            _objectMapper?.let { return it }
            val m = ObjectMapper()
            _objectMapper = m

            // Don't fail on empty POJOs
            m.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            // to prevent exception when encountering unknown property
            m.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

            // Configure Jackson to only use fields for serialization
            val check = VisibilityChecker.Std(
                JsonAutoDetect.Visibility.NONE,  // don't use getters
                JsonAutoDetect.Visibility.NONE,  // don't use getters
                JsonAutoDetect.Visibility.NONE,  // don't use setters
                JsonAutoDetect.Visibility.NONE,  // don't use creators
                JsonAutoDetect.Visibility.ANY    // any field
            )
            m.setVisibility(check)

            // Enable serialization of null and empty values
            m.setSerializationInclusion(JsonInclude.Include.ALWAYS)

            // Add Basic Spigot Types Module
            m.registerModule(JacksonSpigotModule())

            return m
        }

    fun <K, T : Store<T, K>> serializeToDocument(store: T): Document {
        val doc = Document()
        for (provider in store.allFields) {
            val field = provider.fieldWrapper
            val str = serializeFieldProvider(provider)
            doc[field.name] = str
        }
        return doc
    }

    private fun serializeFieldProvider(provider: FieldProvider) : String? {
        val field = provider.fieldWrapper
        val value = field.getNullable()
        return if (value != null) {
            try {
                // Simply serialize to JSON string
                serializeValue(value)
            } catch (e: Exception) {
                kotlin.runCatching {
                    DataStoreSource.colorLogger.error("[JacksonUtil] Error serializing field '${field.name}': ${e.message}")
                }
                throw e
            }
        } else {
            null
        }
    }

    fun <K, T : Store<T, K>> deserializeFromDocument(storeClass: Class<T>, doc: Document): T {
        Preconditions.checkNotNull(doc, "Document cannot be null")

        try {
            val entity = storeClass.getDeclaredConstructor().newInstance() 
                ?: throw RuntimeException("Failed to create new instance of $storeClass")
            entity.initialize()
            entity.readOnly = false

            for (provider in entity.allFields) {
                val field = provider.fieldWrapper
                deserializeFieldWrapper(field, doc)
            }

            entity.readOnly = true
            return entity
        } catch (e: Exception) {
            // promote error upwards
            throw e
        }
    }

    private fun <V> deserializeFieldWrapper(field: FieldWrapper<V>, doc: Document) {
        val fieldName = field.name
        if (doc.containsKey(fieldName)) {
            val rawValue = doc[fieldName]
            if (rawValue != null) {
                try {
                    // Deserialize from JSON string
                    val value = deserializeValue(rawValue as String, field.getFieldType())
                    when (field) {
                        is OptionalField<V> -> field.set(value)
                        is RequiredField<V> -> field.set(value)
                    }
                } catch (e: Exception) {
                    println("Debug - Error deserializing field '${field.name}': ${e.message}")
                    throw e
                }
                return
            } else {
                if (field is OptionalField<V>) {
                    field.set(null)
                    return
                }
            }
        }

        when (field) {
            is OptionalField<V> -> field.set(field.defaultValue)
            is RequiredField<V> -> field.set(field.defaultValue)
        }
    }

    fun <K, T : Store<T, K>> deepCopy(store: T): T {
        val json = serializeToDocument(store).toJson()
        return deserializeFromDocument(store.javaClass, Document.parse(json))
    }
}