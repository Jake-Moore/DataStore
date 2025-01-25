package com.kamikazejam.datastore.util

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.introspect.VisibilityChecker
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.google.common.base.Preconditions
import com.kamikazejam.datastore.DataStoreSource
import com.kamikazejam.datastore.base.Store
import com.kamikazejam.datastore.base.data.CompositeStoreData
import com.kamikazejam.datastore.base.data.SimpleStoreData
import com.kamikazejam.datastore.base.field.*
import com.kamikazejam.datastore.util.jackson.JacksonSpigotModule
import org.bson.Document
import java.util.concurrent.ConcurrentHashMap

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

    fun <K, X : Store<X, K>> serializeToDocument(store: X): Document {
        val doc = Document()
        for (provider in store.allFields) {
            appendFieldProvider(doc, provider)
        }
        return doc
    }

    private fun appendFieldProvider(doc: Document, provider: FieldProvider) {
        val field = provider.fieldWrapper

        // Case 1 - Data is null -> just store null in the json
        val data = field.getNullable()
        if (data == null) {
            doc[field.name] = null
            return
        }

        // Case 2 - Data is not null -> serialize the data by what kind of data it is
        when (data) {
            is SimpleStoreData<*> -> {
                // Just use the serialized value (assumes StoreData returns values compatible BSON)
                // If not, that's the responsibility of the StoreData implementation, and errors will be thrown by MongoDB
                doc[field.name] = data.serializeToBSON()
            }
            is CompositeStoreData<*> -> {
                // We have multiple fields to serialize, so handle each by their own provider
                val subDocument = Document()
                for (subProvider in data.getCustomFields()) {
                    appendFieldProvider(subDocument, subProvider)
                }
                doc[field.name] = subDocument
            }
        }
    }






    private fun serializeFieldWrapperList(wrapper: FieldWrapperList<*>): String {
        return serializeValue(wrapper.toList())
    }

    private fun serializeFieldWrapperSet(wrapper: FieldWrapperSet<*>): String {
        return serializeValue(wrapper.toSet())
    }

    private fun serializeFieldWrapperMap(wrapper: FieldWrapperMap<*, *>): String {
        val map = wrapper.entries.associate { entry ->
            // Serialize both key and value as proper JSON objects
            objectMapper.writeValueAsString(entry.key) to objectMapper.writeValueAsString(entry.value)
        }
        return objectMapper.writeValueAsString(map)
    }

    private fun serializeFieldWrapperConcurrentMap(wrapper: FieldWrapperConcurrentMap<*, *>): String {
        val map = wrapper.entries.associate { entry ->
            // Serialize both key and value as proper JSON objects
            objectMapper.writeValueAsString(entry.key) to objectMapper.writeValueAsString(entry.value)
        }
        return objectMapper.writeValueAsString(map)
    }

    private fun serializeFieldWrapperValue(value: Any): String {
        return serializeValue(value)
    }

    fun <K, X : Store<X, K>> deserializeFromDocument(storeClass: Class<X>, doc: Document): X {
        Preconditions.checkNotNull(doc, "Document cannot be null")

        try {
            val entity = storeClass.getDeclaredConstructor().newInstance() 
                ?: throw RuntimeException("Failed to create new instance of $storeClass")
            entity.initialize()
            entity.readOnly = false

            for (provider in entity.allFields) {
                try {
                    deserializeFieldProvider<Any>(provider, doc)
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

    fun <K, X : Store<X, K>> deepCopy(store: X): X {
        val json = serializeToDocument(store).toJson()
        return deserializeFromDocument(store.javaClass, Document.parse(json))
    }

    @Suppress("UNCHECKED_CAST")
    private fun <V : Any> deserializeFieldProvider(provider: FieldProvider, doc: Document) {
        val field = provider.fieldWrapper as FieldWrapper<V>
        val fieldName = field.name

        // DEFAULT CASE - Field not found in document -> use default value
        if (!doc.containsKey(fieldName)) {
            when (field) {
                is OptionalField<V> -> field.set(field.defaultValue)
                is RequiredField<V> -> field.set(field.defaultValue)
            }
            return
        }

        // NULL CASE - Use null for optional fields, use default value for required fields
        val rawString = doc.getString(fieldName)
        if (rawString == null) {
            when (field) {
                is OptionalField<V> -> field.set(null)
                is RequiredField<V> -> field.set(field.defaultValue)
            }
            return
        }

        // DATA CASE - We must handle this by individual provider types
        try {
            when (provider) {
                is FieldWrapperList<*> -> deserializeFieldWrapperList(provider, rawString)
                is FieldWrapperMap<*, *> -> deserializeFieldWrapperMap(provider, rawString)
                is FieldWrapperSet<*> -> deserializeFieldWrapperSet(provider, rawString)
                is FieldWrapperConcurrentMap<*, *> -> deserializeFieldWrapperConcurrentMap(provider, rawString)
                is FieldWrapper<*> -> deserializeFieldWrapper(provider, rawString)
                else -> throw IllegalStateException("Unknown FieldProvider type: ${provider.javaClass.simpleName}")
            }
        } catch (e: Exception) {
            println("Debug - Error deserializing field '${field.name}': ${e.message}")
            throw e
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun deserializeFieldWrapperList(wrapper: FieldWrapperList<*>, rawString: String) {
        val list = objectMapper.readValue(rawString, List::class.java) as List<*>
        val typedList = ArrayList<Any>()

        // Clear existing list and rebuild it with properly deserialized elements
        wrapper.clear()
        list.forEach { element ->
            // We know the elementType, we need to ensure that all elements got loaded as the correct type
            val deserializedElement = objectMapper.convertValue(element, wrapper.elementType)
            typedList.add(deserializedElement)
        }
        (wrapper.fieldWrapper as RequiredField<Any>).set(typedList)
    }

    @Suppress("UNCHECKED_CAST")
    private fun deserializeFieldWrapperSet(wrapper: FieldWrapperSet<*>, rawString: String) {
        val set = objectMapper.readValue(rawString, Set::class.java) as Set<*>
        val typedSet = HashSet<Any>()

        // Clear existing set and rebuild it with properly deserialized elements
        wrapper.clear()
        set.forEach { element ->
            val deserializedElement = objectMapper.convertValue(element, wrapper.elementType) ?: throw IllegalStateException("Failed to deserialize element in set")
            typedSet.add(deserializedElement)
        }
        (wrapper.fieldWrapper as RequiredField<Any>).set(typedSet)
    }

    @Suppress("UNCHECKED_CAST")
    private fun deserializeFieldWrapperMap(wrapper: FieldWrapperMap<*, *>, rawString: String) {
        val map = objectMapper.readValue(rawString, Map::class.java) as Map<*, *>
        val typedMap = HashMap<Any?, Any?>()

        // Clear existing map and rebuild it with properly deserialized elements
        wrapper.clear()
        map.forEach { (key, value) ->
            // Parse the JSON strings into JsonNode to preserve structure
            val keyNode: JsonNode = objectMapper.readTree(key as String)
            val valueNode: JsonNode = objectMapper.readTree(value as String)
            
            // Convert from JsonNode to the correct types
            val deserializedKey = objectMapper.treeToValue(keyNode, wrapper.keyType)
            val deserializedValue = objectMapper.treeToValue(valueNode, wrapper.valueType)
            typedMap[deserializedKey] = deserializedValue
        }
        (wrapper.fieldWrapper as RequiredField<Any>).set(typedMap)
    }

    @Suppress("UNCHECKED_CAST")
    private fun deserializeFieldWrapperConcurrentMap(wrapper: FieldWrapperConcurrentMap<*, *>, rawString: String) {
        val map = objectMapper.readValue(rawString, Map::class.java) as Map<*, *>
        val typedMap = ConcurrentHashMap<Any?, Any?>()

        // Clear existing map and rebuild it with properly deserialized elements
        wrapper.clear()
        map.forEach { (key, value) ->
            // Parse the JSON strings into JsonNode to preserve structure
            val keyNode: JsonNode = objectMapper.readTree(key as String)
            val valueNode: JsonNode = objectMapper.readTree(value as String)
            
            // Convert from JsonNode to the correct types
            val deserializedKey = objectMapper.treeToValue(keyNode, wrapper.keyType)
            val deserializedValue = objectMapper.treeToValue(valueNode, wrapper.valueType)
            typedMap[deserializedKey] = deserializedValue
        }
        (wrapper.fieldWrapper as RequiredField<Any>).set(typedMap)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <V : Any> deserializeFieldWrapper(wrapper: FieldWrapper<V>, rawString: String) {
        // Handle regular fields
        val field = wrapper.fieldWrapper as FieldWrapper<V>
        val value = deserializeValue(rawString, field.getDataType())
        when (field) {
            is OptionalField<V> -> field.set(value)
            is RequiredField<V> -> field.set(value)
        }
    }
}