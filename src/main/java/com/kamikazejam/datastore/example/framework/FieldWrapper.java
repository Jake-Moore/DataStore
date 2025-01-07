package com.kamikazejam.datastore.example.framework;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Objects;

/**
 * A wrapper for fields that enforces access control based on document state
 */
@JsonSerialize(using = FieldWrapper.FieldWrapperSerializer.class)
@JsonDeserialize(using = FieldWrapper.FieldWrapperDeserializer.class)
public class FieldWrapper<T> {
    private @Nullable T value;
    @JsonIgnore
    private @Nullable BaseDocument<?> parent;
    @Getter
    private @NotNull final String name;
    @Getter
    private @NotNull final Class<T> valueType;

    public FieldWrapper(@NotNull String name, @Nullable T defaultValue, @NotNull Class<T> valueType) {
        this.name = name;
        this.value = defaultValue;
        this.valueType = valueType;
    }

    void setParent(@NotNull BaseDocument<?> parent) {
        this.parent = parent;
    }

    public boolean hasParent() { // TODO REMOVE
        return parent != null;
    }

    public T get() {
        if (parent == null) { throw new IllegalStateException("[FieldWrapper#get] Field not registered with a parent document"); }
        return value;
    }

    public void set(T value) {
        if (parent == null) { throw new IllegalStateException("[FieldWrapper#set] Field not registered with a parent document"); }
        if (parent.isReadOnly()) { throw new IllegalStateException("Cannot modify field '" + name + "' in read-only mode");}
        this.value = value;
    }



    // Serializer to convert FieldWrapper to its value
    public static class FieldWrapperSerializer extends JsonSerializer<FieldWrapper<?>> {
        @Override
        public void serialize(FieldWrapper<?> w, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeStartObject();     // Start the wrapper object
            gen.writeStringField("_class", w.getValueType().getName());
            gen.writeFieldName("value");
            provider.defaultSerializeValue(w.get(), gen);
            gen.writeEndObject();       // End the wrapper object
        }
    }

    @SuppressWarnings("unchecked")
    public static class FieldWrapperDeserializer<T> extends JsonDeserializer<FieldWrapper<T>> {
        @Override
        public FieldWrapper<T> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String fieldName = p.getCurrentName();
            JsonNode node = p.getCodec().readTree(p);
            String className = node.get("_class").asText();
            JsonNode valueNode = node.get("value");

            try {
                Class<T> type = (Class<T>) Class.forName(className);
                T value = (valueNode == null || valueNode.isNull())
                        ? null
                        : p.getCodec().treeToValue(valueNode, type);
                return new FieldWrapper<>(fieldName, value, type);
            } catch (ClassNotFoundException e) {
                throw new IOException("Failed to deserialize FieldWrapper: Class not found - " + className, e);
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FieldWrapper<?> that)) return false;
        return Objects.equals(value, that.value) && Objects.equals(name, that.name) && Objects.equals(valueType, that.valueType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, name, valueType);
    }
}