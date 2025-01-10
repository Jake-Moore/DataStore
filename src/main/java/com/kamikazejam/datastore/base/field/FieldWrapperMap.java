package com.kamikazejam.datastore.base.field;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@SuppressWarnings("unused")
public class FieldWrapperMap<K, V> implements Map<K, V>, FieldProvider {
    private final FieldWrapper<Map<K, V>> wrapper;

    private FieldWrapperMap(@NotNull String name, @Nullable Map<K, V> defaultValue) {
        this.wrapper = FieldWrapper.ofMap(name, defaultValue != null ? new HashMap<>(defaultValue) : new HashMap<>(), Map.class);
    }

    @Override
    @NotNull
    public FieldWrapper<?> getFieldWrapper() {
        return wrapper;
    }

    public static <K, V> FieldWrapperMap<K, V> of(@NotNull String name, @Nullable Map<K, V> defaultValue) {
        return new FieldWrapperMap<>(name, defaultValue);
    }

    private Map<K, V> getModifiableMap() {
        if (!wrapper.isWriteable()) {
            throw new IllegalStateException("Cannot modify map in read-only mode");
        }
        Map<K, V> value = wrapper.get();
        if (value == null) {
            value = new HashMap<>();
            wrapper.set(value);
        }
        return value;
    }

    @Override
    public int size() {
        Map<K, V> value = wrapper.get();
        return value != null ? value.size() : 0;
    }

    @Override
    public boolean isEmpty() {
        Map<K, V> value = wrapper.get();
        return value == null || value.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        Map<K, V> value = wrapper.get();
        return value != null && value.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        Map<K, V> map = wrapper.get();
        return map != null && map.containsValue(value);
    }

    @Override
    public V get(Object key) {
        Map<K, V> value = wrapper.get();
        return value != null ? value.get(key) : null;
    }

    @Nullable
    @Override
    public V put(K key, V value) {
        return getModifiableMap().put(key, value);
    }

    @Override
    public V remove(Object key) {
        return getModifiableMap().remove(key);
    }

    @Override
    public void putAll(@NotNull Map<? extends K, ? extends V> m) {
        getModifiableMap().putAll(m);
    }

    @Override
    public void clear() {
        if (!wrapper.isWriteable()) {
            throw new IllegalStateException("Cannot modify map in read-only mode");
        }
        Map<K, V> value = wrapper.get();
        if (value != null) {
            value.clear();
        }
    }

    @NotNull
    @Override
    public Set<K> keySet() {
        if (!wrapper.isWriteable()) {
            // Return unmodifiable view when in read-only mode
            Map<K, V> value = wrapper.get();
            return value != null ? Set.copyOf(value.keySet()) : Set.of();
        }
        return getModifiableMap().keySet();
    }

    @NotNull
    @Override
    public Collection<V> values() {
        if (!wrapper.isWriteable()) {
            // Return unmodifiable view when in read-only mode
            Map<K, V> value = wrapper.get();
            return value != null ? List.copyOf(value.values()) : List.of();
        }
        return getModifiableMap().values();
    }

    @NotNull
    @Override
    public Set<Entry<K, V>> entrySet() {
        if (!wrapper.isWriteable()) {
            // Return unmodifiable view when in read-only mode
            Map<K, V> value = wrapper.get();
            return value != null ? Set.copyOf(value.entrySet()) : Set.of();
        }
        return getModifiableMap().entrySet();
    }
} 