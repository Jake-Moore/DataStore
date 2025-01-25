package com.kamikazejam.datastore.util

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.introspect.VisibilityChecker
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.google.common.base.Preconditions
import com.kamikazejam.datastore.DataStoreSource
import com.kamikazejam.datastore.base.Store
import com.kamikazejam.datastore.base.data.CompositeStoreData
import com.kamikazejam.datastore.base.data.SimpleStoreData
import com.kamikazejam.datastore.base.data.StoreData
import com.kamikazejam.datastore.base.field.FieldProvider
import com.kamikazejam.datastore.base.field.FieldWrapper
import com.kamikazejam.datastore.base.field.OptionalField
import com.kamikazejam.datastore.base.field.RequiredField
import com.kamikazejam.datastore.util.jackson.JacksonSpigotModule
import org.bson.Document

@Suppress("unused", "DuplicatedCode")
object JacksonUtil {
    const val ID_FIELD: String = "_id"
    const val VERSION_FIELD: String = "version"

    private var _objectMapper: ObjectMapper? = null

    fun serializeValue(value: Any): String {
        return objectMapper.writeValueAsString(value)
    }

    fun <T> deserializeValue(json: String, clazz: Class<T>): T {
        return objectMapper.readValue(json, clazz)
    }

    val objectMapper: ObjectMapper
        get() {
            _objectMapper?.let { return it }
            val m = ObjectMapper().registerKotlinModule()
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


    // ------------------------------------------------------------ //
    //                         SERIALIZATION                        //
    // ------------------------------------------------------------ //
    fun <K : Any, X : Store<X, K>> serializeToDocument(store: X): Document {
        val doc = Document()
        for (provider in store.allFields) {
            appendFieldProvider(doc, provider)
        }
        return doc
    }

    fun appendFieldProvider(doc: Document, provider: FieldProvider) {
        val field = provider.fieldWrapper

        // Case 1 - Data is null -> just store null in the json
        val data = field.getData()
        if (data == null) {
            doc[field.name] = null
            return
        }

        // Case 2 - Data is not null -> serialize the data by what kind of data it is
        val subDocument = Document()
        subDocument[StoreData.TYPE_KEY] = data.getType().name
        when (data) {
            is SimpleStoreData<*> -> {
                // Just use the serialized value (assumes StoreData returns values compatible BSON)
                // If not, that's the responsibility of the StoreData implementation, and errors will be thrown by MongoDB
                subDocument[StoreData.CONTENT_KEY] = data.serializeToBSON()
                doc[field.name] = subDocument
            }
            is CompositeStoreData<*> -> {
                // We have multiple fields to serialize, so handle each by their own provider
                val innerDoc = Document()
                for (subProvider in data.getCustomFields()) {
                    appendFieldProvider(innerDoc, subProvider)
                }

                subDocument[StoreData.CONTENT_KEY] = innerDoc
                doc[field.name] = subDocument
            }
        }
    }



    // ------------------------------------------------------------ //
    //                        DESERIALIZATION                       //
    // ------------------------------------------------------------ //
    fun <K : Any, X : Store<X, K>> deserializeFromDocument(storeClass: Class<X>, doc: Document): X {
        Preconditions.checkNotNull(doc, "Document cannot be null")

        try {
            val entity = storeClass.getDeclaredConstructor().newInstance() 
                ?: throw RuntimeException("Failed to create new instance of $storeClass")
            entity.initialize()
            entity.readOnly = false

            for (provider in entity.allFields) {
                try {
                    deserializeIntoFieldProvider<StoreData<Any>>(provider, doc)
                } catch (e: Exception) {
                    DataStoreSource.colorLogger.error("[JacksonUtil] Error deserializing field '${provider.fieldWrapper.name}': ${e.message}")
                    throw e
                }
            }

            entity.readOnly = true
            return entity
        } catch (e: Exception) {
            // promote error upwards
            throw e
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <D : StoreData<Any>> deserializeIntoFieldProvider(provider: FieldProvider, doc: Document) {
        val field = provider.fieldWrapper as FieldWrapper<D>

        // DEFAULT CASE - Field not found in document -> use default value
        if (!doc.containsKey(field.name)) {
            when (field) {
                is OptionalField<D> -> field.setData(field.defaultValue)
                is RequiredField<D> -> field.setData(field.defaultValue)
            }
            return
        }

        // CASE 1 - We have data, we need to deserialize it
        val subDoc: Document = doc[field.name] as Document
        val type = StoreData.Companion.Type.valueOf(subDoc.getString(StoreData.TYPE_KEY))

        val data: D = field.creator.invoke()
        when (type) {
            StoreData.Companion.Type.SIMPLE -> {
                if (data !is SimpleStoreData<*>) throw IllegalStateException("Field '${field.name}' is not a SimpleStoreData")

                // Deserialize the data in the CONTENT_KEY, using the data class itself
                data.deserializeFromBSON(subDoc, StoreData.CONTENT_KEY)
            }
            StoreData.Companion.Type.COMPOSITE -> {
                if (data !is CompositeStoreData<*>) throw IllegalStateException("Field '${field.name}' is not a CompositeStoreData")

                // Handle Composite Type
                val innerDoc: Document = subDoc[StoreData.CONTENT_KEY] as Document
                for (subProvider in data.getCustomFields()) {
                    deserializeIntoFieldProvider<StoreData<Any>>(subProvider, innerDoc)
                }
            }
        }

        when (field) {
            is OptionalField<D> -> field.setData(data)
            is RequiredField<D> -> field.setData(data)
        }
    }



    // ------------------------------------------------------------ //
    //                             UTIL                             //
    // ------------------------------------------------------------ //
    fun <K : Any, X : Store<X, K>> deepCopy(store: X): X {
        val json = serializeToDocument(store).toJson()
        return deserializeFromDocument(store.javaClass, Document.parse(json))
    }
}