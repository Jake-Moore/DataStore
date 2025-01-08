package com.kamikazejam.datastore.util;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.google.common.base.Preconditions;
import com.kamikazejam.datastore.base.Store;
import com.kamikazejam.datastore.base.field.FieldWrapper;
import com.kamikazejam.datastore.util.jackson.JacksonSpigotModule;
import lombok.SneakyThrows;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

@SuppressWarnings("unused")
public class JacksonUtil {
    public static final @NotNull String ID_FIELD = "_id";

    private static @Nullable ObjectMapper mapper = null;
    public static @NotNull ObjectMapper getObjectMapper() {
        if (mapper != null) return mapper;
        mapper = new ObjectMapper();

        // Optional: enable pretty printing
        // mapper.enable(SerializationFeature.INDENT_OUTPUT);

        // Don't fail on empty POJOs
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        // to prevent exception when encountering unknown property:
        //  i.e. if the json has a property no longer in the class
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        // Configure Jackson to only use fields for serialization (ignoring transient fields)
        //   We have to disable setters and getters, otherwise a transient getter or setter will cause it to be serialized
        VisibilityChecker.Std check = new VisibilityChecker.Std(
                JsonAutoDetect.Visibility.NONE,           // don't use getters for field mapping
                JsonAutoDetect.Visibility.NONE,           // don't use getters for field mapping
                JsonAutoDetect.Visibility.NONE,           // don't use setters for field mapping
                JsonAutoDetect.Visibility.NONE,           // don't use creators
                JsonAutoDetect.Visibility.ANY             // any field
        );
        mapper.setVisibility(check);

        // Enable serialization of null and empty values
        mapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);

        // Add Basic Spigot Types Module, for handling basic types
        mapper.registerModule(new JacksonSpigotModule());
        
        return mapper;
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
    public static <K, T extends Store<K>> Document serializeToDocument(@NotNull T store) {
        Preconditions.checkNotNull(store, "Entity cannot be null");

        Document doc = new Document();
        for (FieldWrapper<?> field : store.getAllFields()) {
            Object value = field.get();
            if (value != null) {
                // Convert the value to a MongoDB-compatible format using Jackson
                Object convertedValue = getObjectMapper().convertValue(value, Object.class);
                // If the converted value is a Map, convert it to a (sub) Document
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

    @NotNull
    public static <K, T extends Store<K>> T deserializeFromDocument(@NotNull Class<T> storeClass, @NotNull Document doc) {
        Preconditions.checkNotNull(doc, "Document cannot be null");

        try {
            T entity = storeClass.getDeclaredConstructor().newInstance();
            entity.initialize();
            entity.setReadOnly(false);

            // Deserialize each FieldWrapper from its contents in the BSON document
            for (FieldWrapper<?> field : entity.getAllFields()) {
                deserializeFieldWrapper(field, doc);
            }

            entity.setReadOnly(true);
            return entity;
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize document", e);
        }
    }

    private static <V> void deserializeFieldWrapper(FieldWrapper<V> field, Document doc) {
        String fieldName = field.getName();
        if (doc.containsKey(fieldName)) {
            Object rawValue = doc.get(fieldName);
            if (rawValue != null) {
                // Convert the MongoDB value to the proper type using Jackson
                V value = getObjectMapper().convertValue(rawValue, field.getValueType());
                field.set(value);
            } else {
                field.set(null);
            }
        } else {
            field.set(field.getDefaultValue());
        }
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public static <K, T extends Store<K>> T deepCopy(@NotNull T store) {
        String json = JacksonUtil.serializeToDocument(store).toJson();
        return (T) JacksonUtil.deserializeFromDocument(store.getClass(), Document.parse(json));
    }
} 