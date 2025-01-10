package com.kamikazejam.datastore.base.field;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

@SuppressWarnings("unused")
public class FieldWrapperSet<E> implements Set<E> {
    private final FieldWrapper<Set<E>> wrapper;

    private FieldWrapperSet(@NotNull String name, @Nullable Set<E> defaultValue) {
        this.wrapper = FieldWrapper.ofColl(name, defaultValue != null ? new HashSet<>(defaultValue) : new HashSet<>(), Set.class);
    }

    public static <E> FieldWrapperSet<E> of(@NotNull String name, @Nullable Set<E> defaultValue) {
        return new FieldWrapperSet<>(name, defaultValue);
    }

    private Set<E> getModifiableSet() {
        if (!wrapper.isWriteable()) {
            throw new IllegalStateException("Cannot modify set in read-only mode");
        }
        Set<E> value = wrapper.get();
        if (value == null) {
            value = new HashSet<>();
            wrapper.set(value);
        }
        return value;
    }

    @Override
    public int size() {
        Set<E> value = wrapper.get();
        return value != null ? value.size() : 0;
    }

    @Override
    public boolean isEmpty() {
        Set<E> value = wrapper.get();
        return value == null || value.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        Set<E> value = wrapper.get();
        return value != null && value.contains(o);
    }

    @NotNull
    @Override
    public Iterator<E> iterator() {
        if (!wrapper.isWriteable()) {
            // Return unmodifiable iterator when in read-only mode
            Set<E> value = wrapper.get();
            return value != null ? Set.copyOf(value).iterator() : Set.<E>of().iterator();
        }
        return getModifiableSet().iterator();
    }

    @NotNull
    @Override
    public Object @NotNull [] toArray() {
        Set<E> value = wrapper.get();
        return value != null ? value.toArray() : new Object[0];
    }

    @NotNull
    @Override
    public <T> T @NotNull [] toArray(@NotNull T @NotNull [] a) {
        Set<E> value = wrapper.get();
        return value != null ? value.toArray(a) : a;
    }

    @Override
    public boolean add(E e) {
        return getModifiableSet().add(e);
    }

    @Override
    public boolean remove(Object o) {
        return getModifiableSet().remove(o);
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        Set<E> value = wrapper.get();
        return value != null && value.containsAll(c);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends E> c) {
        return getModifiableSet().addAll(c);
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        return getModifiableSet().retainAll(c);
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        return getModifiableSet().removeAll(c);
    }

    @Override
    public void clear() {
        if (!wrapper.isWriteable()) {
            throw new IllegalStateException("Cannot modify set in read-only mode");
        }
        Set<E> value = wrapper.get();
        if (value != null) {
            value.clear();
        }
    }
} 