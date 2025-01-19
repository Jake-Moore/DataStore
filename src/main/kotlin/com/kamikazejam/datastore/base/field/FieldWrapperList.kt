@file:Suppress("UNCHECKED_CAST")

package com.kamikazejam.datastore.base.field

interface FieldWrapperList<E> : MutableList<E>, FieldProvider {
    val elementType: Class<*>
    companion object {
        @JvmStatic
        fun <E> of(name: String, elementType: Class<E>, defaultValue: List<E> = mutableListOf()): FieldWrapperList<E> =
            FieldWrapperListImpl(name, defaultValue, elementType)
    }
}

private class FieldWrapperListImpl<E>(
    name: String,
    defaultValue: List<E>,
    override val elementType: Class<E>
) : FieldWrapperList<E> {
    private var wrapper = RequiredField.of(
        name,
        ArrayList(defaultValue),
        ArrayList::class.java as Class<ArrayList<E>>
    )

    override val fieldWrapper: FieldWrapper<*>
        get() = wrapper

    private fun getModifiableList(): MutableList<E> {
        if (!wrapper.isWriteable) {
            throw IllegalStateException("Cannot modify list in read-only mode")
        }
        return wrapper.get()
    }

    override val size: Int
        get() = wrapper.get().size

    override fun isEmpty(): Boolean = wrapper.get().isEmpty()

    override fun contains(element: E): Boolean = wrapper.get().contains(element)

    override fun iterator(): MutableIterator<E> = if (!wrapper.isWriteable) {
        ArrayList(wrapper.get().toList()).iterator()
    } else {
        getModifiableList().iterator()
    }

    override fun add(element: E): Boolean = getModifiableList().add(element)

    override fun remove(element: E): Boolean = getModifiableList().remove(element)

    override fun containsAll(elements: Collection<E>): Boolean = wrapper.get().containsAll(elements)

    override fun addAll(elements: Collection<E>): Boolean = getModifiableList().addAll(elements)

    override fun addAll(index: Int, elements: Collection<E>): Boolean =
        getModifiableList().addAll(index, elements)

    override fun removeAll(elements: Collection<E>): Boolean = getModifiableList().removeAll(elements.toSet())

    override fun retainAll(elements: Collection<E>): Boolean = getModifiableList().retainAll(elements.toSet())

    override fun clear() {
        if (!wrapper.isWriteable) {
            throw IllegalStateException("Cannot modify list in read-only mode")
        }
        wrapper.get().clear()
    }

    override fun get(index: Int): E = wrapper.get()[index]

    override fun set(index: Int, element: E): E = getModifiableList().set(index, element)

    override fun add(index: Int, element: E) = getModifiableList().add(index, element)

    override fun removeAt(index: Int): E = getModifiableList().removeAt(index)

    override fun indexOf(element: E): Int = wrapper.get().indexOf(element)

    override fun lastIndexOf(element: E): Int = wrapper.get().lastIndexOf(element)

    override fun listIterator(): MutableListIterator<E> = if (!wrapper.isWriteable) {
        ArrayList(wrapper.get().toList()).listIterator()
    } else {
        getModifiableList().listIterator()
    }

    override fun listIterator(index: Int): MutableListIterator<E> = if (!wrapper.isWriteable) {
        ArrayList(wrapper.get().toList()).listIterator(index)
    } else {
        getModifiableList().listIterator(index)
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<E> = if (!wrapper.isWriteable) {
        ArrayList(wrapper.get().subList(fromIndex, toIndex))
    } else {
        getModifiableList().subList(fromIndex, toIndex)
    }
} 