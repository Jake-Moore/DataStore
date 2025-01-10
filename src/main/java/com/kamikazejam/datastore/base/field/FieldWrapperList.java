package com.kamikazejam.datastore.base.field;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@SuppressWarnings("unused")
public class FieldWrapperList<E> implements List<E> {
    private final FieldWrapper<List<E>> wrapper;

    private FieldWrapperList(@NotNull String name, @Nullable List<E> defaultValue) {
        this.wrapper = FieldWrapper.ofColl(name, defaultValue != null ? new ArrayList<>(defaultValue) : new ArrayList<>(), List.class);
    }

    public static <E> FieldWrapperList<E> of(@NotNull String name, @Nullable List<E> defaultValue) {
        return new FieldWrapperList<>(name, defaultValue);
    }

    private List<E> getModifiableList() {
        if (!wrapper.isWriteable()) {
            throw new IllegalStateException("Cannot modify list in read-only mode");
        }
        List<E> value = wrapper.get();
        if (value == null) {
            value = new ArrayList<>();
            wrapper.set(value);
        }
        return value;
    }

    @Override
    public int size() {
        List<E> value = wrapper.get();
        return value != null ? value.size() : 0;
    }

    @Override
    public boolean isEmpty() {
        List<E> value = wrapper.get();
        return value == null || value.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        List<E> value = wrapper.get();
        return value != null && value.contains(o);
    }

    @NotNull
    @Override
    public Iterator<E> iterator() {
        if (!wrapper.isWriteable()) {
            // Return unmodifiable iterator when in read-only mode
            List<E> value = wrapper.get();
            return value != null ? List.copyOf(value).iterator() : Collections.emptyIterator();
        }
        return getModifiableList().iterator();
    }

    @NotNull
    @Override
    public Object @NotNull [] toArray() {
        List<E> value = wrapper.get();
        return value != null ? value.toArray() : new Object[0];
    }

    @NotNull
    @Override
    public <T> T @NotNull [] toArray(@NotNull T @NotNull [] a) {
        List<E> value = wrapper.get();
        return value != null ? value.toArray(a) : a;
    }

    @Override
    public boolean add(E e) {
        return getModifiableList().add(e);
    }

    @Override
    public boolean remove(Object o) {
        return getModifiableList().remove(o);
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        List<E> value = wrapper.get();
        return value != null && new HashSet<>(value).containsAll(c);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends E> c) {
        return getModifiableList().addAll(c);
    }

    @Override
    public boolean addAll(int index, @NotNull Collection<? extends E> c) {
        return getModifiableList().addAll(index, c);
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        return getModifiableList().removeAll(c);
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        return getModifiableList().retainAll(c);
    }

    @Override
    public void clear() {
        if (!wrapper.isWriteable()) {
            throw new IllegalStateException("Cannot modify list in read-only mode");
        }
        List<E> value = wrapper.get();
        if (value != null) {
            value.clear();
        }
    }

    @Override
    public E get(int index) {
        List<E> value = wrapper.get();
        if (value == null) {
            throw new IndexOutOfBoundsException("List is null");
        }
        return value.get(index);
    }

    @Override
    public E set(int index, E element) {
        return getModifiableList().set(index, element);
    }

    @Override
    public void add(int index, E element) {
        getModifiableList().add(index, element);
    }

    @Override
    public E remove(int index) {
        return getModifiableList().remove(index);
    }

    @Override
    public int indexOf(Object o) {
        List<E> value = wrapper.get();
        return value != null ? value.indexOf(o) : -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        List<E> value = wrapper.get();
        return value != null ? value.lastIndexOf(o) : -1;
    }

    @NotNull
    @Override
    public ListIterator<E> listIterator() {
        if (!wrapper.isWriteable()) {
            // Return unmodifiable iterator when in read-only mode
            List<E> value = wrapper.get();
            return value != null ? List.copyOf(value).listIterator() : List.<E>of().listIterator();
        }
        return getModifiableList().listIterator();
    }

    @NotNull
    @Override
    public ListIterator<E> listIterator(int index) {
        if (!wrapper.isWriteable()) {
            // Return unmodifiable iterator when in read-only mode
            List<E> value = wrapper.get();
            return value != null ? List.copyOf(value).listIterator(index) : List.<E>of().listIterator();
        }
        return getModifiableList().listIterator(index);
    }

    @NotNull
    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        if (!wrapper.isWriteable()) {
            // Return unmodifiable view when in read-only mode
            List<E> value = wrapper.get();
            if (value == null) {
                throw new IndexOutOfBoundsException("List is null");
            }
            return List.copyOf(value.subList(fromIndex, toIndex));
        }
        return getModifiableList().subList(fromIndex, toIndex);
    }
} 