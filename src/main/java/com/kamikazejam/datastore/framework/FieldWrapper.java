package com.kamikazejam.datastore.framework;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;

/**
 * A wrapper for fields that enforces access control based on document state
 */
@SuppressWarnings("unchecked")
public class FieldWrapper<T> {
    private final UUID uuid = UUID.randomUUID();

    private @Nullable T value;
    @Getter
    private final @Nullable T defaultValue;
    @JsonIgnore
    private @Nullable BaseDocument<?> parent;
    private @NotNull final String name;
    private @NotNull final Class<?> valueType;

    private FieldWrapper(@NotNull String name, @Nullable T defaultValue, @NotNull Class<T> valueType) {
        this(name, defaultValue, valueType, null);
    }

    @SuppressWarnings("unused")
    private FieldWrapper(@NotNull String name, @Nullable T defaultValue, @NotNull Class<?> valueType, @Nullable Class<?> elementType) {
        this.name = name;
        this.value = defaultValue;
        this.defaultValue = defaultValue;
        this.valueType = valueType;
        // We don't actually need `elementType` in our implementation yet, but we
        //  keep it in case it is needed in the future
    }


    // ------------------------------------------------------ //
    // Static Constructors                                    //
    // ------------------------------------------------------ //

    public static <T> FieldWrapper<T> of(@NotNull String name, @Nullable T defaultValue, @NotNull Class<T> valueType) {
        return new FieldWrapper<>(name, defaultValue, valueType);
    }

    // Generic constructor for any collection type
    public static <C extends Collection<E>, E> FieldWrapper<C> ofColl(
            @NotNull String name,
            @Nullable C defaultValue,
            @NotNull Class<? super C> collectionType
    ) {
        return new FieldWrapper<>(name, defaultValue, collectionType, null);
    }

    // Generic constructor for any map type
    public static <K, V, M extends Map<K,V>> FieldWrapper<M> ofMap(
            @NotNull String name,
            @Nullable M defaultValue,
            @NotNull Class<? super M> mapType
    ) {
        return new FieldWrapper<>(name, defaultValue, mapType, null);
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public Class<T> getValueType() {
        return (Class<T>) valueType;
    }

    void setParent(@NotNull BaseDocument<?> parent) {
        this.parent = parent;
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