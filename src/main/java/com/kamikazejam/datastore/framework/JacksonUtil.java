package com.kamikazejam.datastore.framework;

import java.util.Map;

import org.bson.Document;
import org.jetbrains.annotations.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.base.Preconditions;

import lombok.SneakyThrows;

@SuppressWarnings("unused")
public class JacksonUtil {
    private static final ObjectMapper OBJECT_MAPPER = createObjectMapper();
    
    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // Configure visibility
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        
        // Configure features
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        
        // Register custom module for FieldWrapper
        SimpleModule module = new SimpleModule();
        mapper.registerModule(module);
        
        return mapper;
    }
    
    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }

    @SneakyThrows
    public static <T> @NotNull String toJson(@NotNull FieldWrapper<T> wrapper) {
        Preconditions.checkNotNull(wrapper, "wrapper cannot be null");
        return getObjectMapper().writeValueAsString(wrapper.get());
    }

    @SneakyThrows
    public static <T> @NotNull T fromJson(@NotNull FieldWrapper<T> wrapper, @NotNull String json) {
        Preconditions.checkNotNull(json, "json cannot be null");
        Preconditions.checkNotNull(wrapper, "wrapper cannot be null");
        return getObjectMapper().readValue(json, wrapper.getValueType());
    }

    @NotNull
    @SneakyThrows
    @SuppressWarnings("unchecked")
    public static <T extends BaseDocument<T>> Document serializeToDocument(@NotNull T entity) {
        Preconditions.checkNotNull(entity, "Entity cannot be null");

        Document doc = new Document();
        for (FieldWrapper<?> field : entity.getAllFields()) {
            Object value = field.get();

            // Special Handling for _id to make it searchable
            if (field.getName().equals("_id") && value instanceof String s) {
                doc.put("_id", s);
                continue;
            }

            if (value != null) {
                // Convert the value to a MongoDB-compatible format using Jackson
                Object convertedValue = getObjectMapper().convertValue(value, Object.class);
                // If the converted value is a Map, convert it to a Document
                if (convertedValue instanceof Map<?, ?> map) {
                    convertedValue = new Document((Map<String, Object>) map);
                }
                doc.put(field.getName(), convertedValue);
            } else {
                doc.put(field.getName(), null);
            }
        }
        return doc;
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public static <T extends BaseDocument<T>> T deserializeFromDocument(@NotNull Class<T> entityClass, @NotNull Document doc) {
        Preconditions.checkNotNull(doc, "Document cannot be null");

        try {
            T entity = entityClass.getDeclaredConstructor().newInstance();
            entity.initialize();
            entity.setModifiable();

            // For each field in the document, find the corresponding FieldWrapper and set its value
            for (FieldWrapper<?> field : entity.getAllFields()) {
                String fieldName = field.getName();
                FieldWrapper<Object> typedField = (FieldWrapper<Object>) field;

                if (doc.containsKey(fieldName)) {
                    Object rawValue = doc.get(fieldName);
                    if (rawValue != null) {
                        // Convert the MongoDB value to the proper type using Jackson
                        Object value = getObjectMapper().convertValue(rawValue, field.getValueType());
                        typedField.set(value);
                    } else {
                        typedField.set(null);
                    }
                } else {
                    typedField.set(typedField.getDefaultValue());
                }
            }

            entity.setReadOnly();
            return entity;
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize document", e);
        }
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public static <T extends BaseDocument<T>> T deepCopy(@NotNull T entity) {
        String json = JacksonUtil.serializeToDocument(entity).toJson();
        return (T) JacksonUtil.deserializeFromDocument(entity.getClass(), Document.parse(json));
    }
} 