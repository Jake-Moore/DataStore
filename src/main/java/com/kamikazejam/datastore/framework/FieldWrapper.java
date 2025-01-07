package com.kamikazejam.datastore.framework;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

/**
 * A wrapper for fields that enforces access control based on document state
 */
public class FieldWrapper<T> {
    private final UUID uuid = UUID.randomUUID();

    private @Nullable T value;
    @Getter
    private final @Nullable T defaultValue;
    @JsonIgnore
    private @Nullable BaseDocument<?> parent;
    private @NotNull final String name;
    private @NotNull final Class<T> valueType;

    public FieldWrapper(@NotNull String name, @Nullable T defaultValue, @NotNull Class<T> valueType) {
        this.name = name;
        this.value = defaultValue;
        this.defaultValue = defaultValue;
        this.valueType = valueType;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public Class<T> getValueType() {
        return valueType;
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