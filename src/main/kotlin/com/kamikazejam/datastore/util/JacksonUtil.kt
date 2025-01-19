package com.kamikazejam.datastore.util

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.introspect.VisibilityChecker
import com.google.common.base.Preconditions
import com.kamikazejam.datastore.base.Store
import com.kamikazejam.datastore.base.field.FieldWrapper
import com.kamikazejam.datastore.base.field.OptionalField
import com.kamikazejam.datastore.base.field.RequiredField
import com.kamikazejam.datastore.util.jackson.JacksonSpigotModule
import de.undercouch.bson4jackson.BsonFactory
import de.undercouch.bson4jackson.BsonGenerator
import org.bson.Document
import java.io.ByteArrayOutputStream

@Suppress("unused")
object JacksonUtil {
    const val ID_FIELD: String = "_id"

    private var _objectMapper: ObjectMapper? = null

    val objectMapper: ObjectMapper
        get() {
            _objectMapper?.let { return it }
            val factory = BsonFactory()
            factory.enable(BsonGenerator.Feature.ENABLE_STREAMING)
            val m = ObjectMapper(factory)
            _objectMapper = m

            // Don't fail on empty POJOs
            m.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)

            // to prevent exception when encountering unknown property:
            //  i.e. if the json has a property no longer in the class
            m.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

            // Configure Jackson to only use fields for serialization (ignoring transient fields)
            val check = VisibilityChecker.Std(
                JsonAutoDetect.Visibility.NONE,  // don't use getters for field mapping
                JsonAutoDetect.Visibility.NONE,  // don't use getters for field mapping
                JsonAutoDetect.Visibility.NONE,  // don't use setters for field mapping
                JsonAutoDetect.Visibility.NONE,  // don't use creators
                JsonAutoDetect.Visibility.ANY // any field
            )
            m.setVisibility(check)

            // Enable serialization of null and empty values
            m.setSerializationInclusion(JsonInclude.Include.ALWAYS)

            // Add Basic Spigot Types Module, for handling basic types
            m.registerModule(JacksonSpigotModule())

            return m
        }

    fun <T> toJson(wrapper: FieldWrapper<T>): String {
        Preconditions.checkNotNull(wrapper, "wrapper cannot be null")
        return when (wrapper) {
            is OptionalField<*> -> objectMapper.writeValueAsString(wrapper.get())
            is RequiredField<*> -> objectMapper.writeValueAsString(wrapper.get())
        }
    }

    fun <T> fromJson(wrapper: FieldWrapper<T>, json: String): T {
        Preconditions.checkNotNull(json, "json cannot be null")
        Preconditions.checkNotNull(wrapper, "wrapper cannot be null")
        return objectMapper.readValue(json, wrapper.getFieldType())
    }

    fun <K, T : Store<T, K>> serializeToDocument(store: T): Document {
        val doc = Document()
        for (provider in store.allFields) {
            val field = provider.fieldWrapper
            val value = field.getNullable()
            if (value != null) {
                // Serialize to BSON bytes
                val byteArray = ByteArrayOutputStream()
                objectMapper.writeValue(byteArray, value)
                // Parse bytes into Document
                val bsonValue = Document.parse(byteArray.toString("UTF-8"))
                doc[field.name] = if (bsonValue.size == 1) bsonValue.values.first() else bsonValue
            } else {
                doc[field.name] = null
            }
        }
        return doc
    }

    fun <K, T : Store<T, K>> deserializeFromDocument(storeClass: Class<T>, doc: Document): T {
        Preconditions.checkNotNull(doc, "Document cannot be null")

        try {
            val entity = storeClass.getDeclaredConstructor().newInstance() ?: throw RuntimeException("Failed to create new instance of $storeClass")
            entity.initialize()
            entity.readOnly = false

            // Deserialize each FieldWrapper from its contents in the BSON document
            for (provider in entity.allFields) {
                val field = provider.fieldWrapper
                deserializeFieldWrapper(field, doc)
            }

            entity.readOnly = true
            return entity
        } catch (e: Exception) {
            throw RuntimeException("Failed to deserialize document", e)
        }
    }

    private fun <V> deserializeFieldWrapper(field: FieldWrapper<V>, doc: Document) {
        val fieldName = field.name
        if (doc.containsKey(fieldName)) {
            val rawValue = doc[fieldName]
            if (rawValue != null) {
                // Convert to BSON bytes first
                val byteArray = ByteArrayOutputStream()
                objectMapper.writeValue(byteArray, rawValue)
                // Read value directly from bytes
                val value = objectMapper.readValue(byteArray.toByteArray(), field.getFieldType())
                
                when (field) {
                    is OptionalField<V> -> field.set(value)
                    is RequiredField<V> -> field.set(value)
                }
                return
            } else {
                // If we have OptionalField, we can set null
                if (field is OptionalField<V>) {
                    field.set(null)
                    return
                }
            }
        }

        // Use default if there was nothing serialized or it was null
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