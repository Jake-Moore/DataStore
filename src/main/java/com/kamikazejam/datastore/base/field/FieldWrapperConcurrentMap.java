package com.kamikazejam.datastore.base.field;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@SuppressWarnings("unused")
public class FieldWrapperConcurrentMap<K, V> implements ConcurrentMap<K, V> {
    private final FieldWrapper<ConcurrentMap<K, V>> wrapper;

    private FieldWrapperConcurrentMap(@NotNull String name, @Nullable ConcurrentMap<K, V> defaultValue) {
        this.wrapper = FieldWrapper.ofMap(name, defaultValue != null ? new ConcurrentHashMap<>(defaultValue) : new ConcurrentHashMap<>(), ConcurrentMap.class);
    }

    public static <K, V> FieldWrapperConcurrentMap<K, V> of(@NotNull String name, @Nullable ConcurrentMap<K, V> defaultValue) {
        return new FieldWrapperConcurrentMap<>(name, defaultValue);
    }

    private ConcurrentMap<K, V> getModifiableMap() {
        if (!wrapper.isWriteable()) {
            throw new IllegalStateException("Cannot modify map in read-only mode");
        }
        ConcurrentMap<K, V> value = wrapper.get();
        if (value == null) {
            value = new ConcurrentHashMap<>();
            wrapper.set(value);
        }
        return value;
    }

    @Override
    public int size() {
        ConcurrentMap<K, V> value = wrapper.get();
        return value != null ? value.size() : 0;
    }

    @Override
    public boolean isEmpty() {
        ConcurrentMap<K, V> value = wrapper.get();
        return value == null || value.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        ConcurrentMap<K, V> value = wrapper.get();
        return value != null && value.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        ConcurrentMap<K, V> map = wrapper.get();
        return map != null && map.containsValue(value);
    }

    @Override
    public V get(Object key) {
        ConcurrentMap<K, V> value = wrapper.get();
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
        ConcurrentMap<K, V> value = wrapper.get();
        if (value != null) {
            value.clear();
        }
    }

    @NotNull
    @Override
    public Set<K> keySet() {
        if (!wrapper.isWriteable()) {
            // Return unmodifiable view when in read-only mode
            ConcurrentMap<K, V> value = wrapper.get();
            return value != null ? Set.copyOf(value.keySet()) : Set.of();
        }
        return getModifiableMap().keySet();
    }

    @NotNull
    @Override
    public Collection<V> values() {
        if (!wrapper.isWriteable()) {
            // Return unmodifiable view when in read-only mode
            ConcurrentMap<K, V> value = wrapper.get();
            return value != null ? List.copyOf(value.values()) : List.of();
        }
        return getModifiableMap().values();
    }

    @NotNull
    @Override
    public Set<Entry<K, V>> entrySet() {
        if (!wrapper.isWriteable()) {
            // Return unmodifiable view when in read-only mode
            ConcurrentMap<K, V> value = wrapper.get();
            return value != null ? Set.copyOf(value.entrySet()) : Set.of();
        }
        return getModifiableMap().entrySet();
    }

    @Override
    public V putIfAbsent(@NotNull K key, V value) {
        return getModifiableMap().putIfAbsent(key, value);
    }

    @Override
    public boolean remove(@NotNull Object key, Object value) {
        return getModifiableMap().remove(key, value);
    }

    @Override
    public boolean replace(@NotNull K key, @NotNull V oldValue, @NotNull V newValue) {
        return getModifiableMap().replace(key, oldValue, newValue);
    }

    @Override
    public V replace(@NotNull K key, @NotNull V value) {
        return getModifiableMap().replace(key, value);
    }
} 