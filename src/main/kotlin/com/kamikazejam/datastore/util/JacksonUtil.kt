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
import com.kamikazejam.datastore.util.jackson.JacksonSpigotModule
import org.bson.Document

@Suppress("unused")
object JacksonUtil {
    const val ID_FIELD: String = "_id"

    private var mapper: ObjectMapper? = null
    val objectMapper: ObjectMapper
        get() {
            if (mapper != null) return mapper!!
            mapper = ObjectMapper()

            // Optional: enable pretty printing
            // mapper.enable(SerializationFeature.INDENT_OUTPUT);

            // Don't fail on empty POJOs
            mapper!!.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)

            // to prevent exception when encountering unknown property:
            //  i.e. if the json has a property no longer in the class
            mapper!!.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

            // Configure Jackson to only use fields for serialization (ignoring transient fields)
            //   We have to disable setters and getters, otherwise a transient getter or setter will cause it to be serialized
            val check =
                VisibilityChecker.Std(
                    JsonAutoDetect.Visibility.NONE,  // don't use getters for field mapping
                    JsonAutoDetect.Visibility.NONE,  // don't use getters for field mapping
                    JsonAutoDetect.Visibility.NONE,  // don't use setters for field mapping
                    JsonAutoDetect.Visibility.NONE,  // don't use creators
                    JsonAutoDetect.Visibility.ANY // any field
                )
            mapper!!.setVisibility(check)

            // Enable serialization of null and empty values
            mapper!!.setSerializationInclusion(JsonInclude.Include.ALWAYS)

            // Add Basic Spigot Types Module, for handling basic types
            mapper!!.registerModule(JacksonSpigotModule())

            return mapper!!
        }

    fun <T> toJson(wrapper: FieldWrapper<T>): String {
        Preconditions.checkNotNull(wrapper, "wrapper cannot be null")
        return objectMapper.writeValueAsString(wrapper.get())
    }

    fun <T> fromJson(wrapper: FieldWrapper<T>, json: String): T {
        Preconditions.checkNotNull(json, "json cannot be null")
        Preconditions.checkNotNull(wrapper, "wrapper cannot be null")
        return objectMapper.readValue(json, wrapper.getValueType())
    }

    @Suppress("UNCHECKED_CAST")
    fun <K, T : Store<T, K>> serializeToDocument(store: T): Document {
        val doc = Document()
        for (provider in store.allFields) {
            val field = provider.fieldWrapper
            val value = field.get()
            if (value != null) {
                // Convert the value to a MongoDB-compatible format using Jackson
                var convertedValue: Any = objectMapper.convertValue(value, Any::class.java)
                // If the converted value is a Map, convert it to a (sub) Document
                if (convertedValue is Map<*, *>) {
                    convertedValue = Document(convertedValue as Map<String?, Any?>)
                }
                doc[field.name] = convertedValue
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
                // Convert the MongoDB value to the proper type using Jackson
                val value = objectMapper.convertValue(rawValue, field.getValueType())
                field.set(value)
            } else {
                field.set(null)
            }
        } else {
            field.set(field.defaultValue)
        }
    }

    fun <K, T : Store<T, K>> deepCopy(store: T): T {
        val json = serializeToDocument(store).toJson()
        return deserializeFromDocument(store.javaClass, Document.parse(json))
    }
}