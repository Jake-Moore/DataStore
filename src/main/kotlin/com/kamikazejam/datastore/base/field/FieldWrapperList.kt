package com.kamikazejam.datastore.base.field

@Suppress("unused")
class FieldWrapperList<E> private constructor(
    name: String,
    defaultValue: List<E>?
) : MutableList<E>, FieldProvider {
    private var wrapper = FieldWrapper.ofColl(
        name,
        defaultValue?.let { ArrayList(it) } ?: ArrayList(),
        List::class.java
    )

    override val fieldWrapper: FieldWrapper<*>
        get() = wrapper

    private fun getModifiableList(): MutableList<E> {
        if (!wrapper.isWriteable) {
            throw IllegalStateException("Cannot modify list in read-only mode")
        }
        return wrapper.get() ?: ArrayList<E>().also { wrapper.set(it) }
    }

    override val size: Int
        get() = wrapper.get()?.size ?: 0

    override fun isEmpty(): Boolean = wrapper.get()?.isEmpty() ?: true

    override fun contains(element: E): Boolean = wrapper.get()?.contains(element) ?: false

    override fun iterator(): MutableIterator<E> = if (!wrapper.isWriteable) {
        ArrayList(wrapper.get()?.toList() ?: emptyList()).iterator()
    } else {
        getModifiableList().iterator()
    }

    override fun add(element: E): Boolean = getModifiableList().add(element)

    override fun remove(element: E): Boolean = getModifiableList().remove(element)

    override fun containsAll(elements: Collection<E>): Boolean =
        wrapper.get()?.containsAll(elements) ?: false

    override fun addAll(elements: Collection<E>): Boolean = getModifiableList().addAll(elements)

    override fun addAll(index: Int, elements: Collection<E>): Boolean =
        getModifiableList().addAll(index, elements)

    override fun removeAll(elements: Collection<E>): Boolean = getModifiableList().removeAll(elements.toSet())

    override fun retainAll(elements: Collection<E>): Boolean = getModifiableList().retainAll(elements.toSet())

    override fun clear() {
        if (!wrapper.isWriteable) {
            throw IllegalStateException("Cannot modify list in read-only mode")
        }
        wrapper.get()?.clear()
    }

    override fun get(index: Int): E {
        return wrapper.get()?.get(index) ?: throw IndexOutOfBoundsException("List is null")
    }

    override fun set(index: Int, element: E): E = getModifiableList().set(index, element)

    override fun add(index: Int, element: E) = getModifiableList().add(index, element)

    override fun removeAt(index: Int): E = getModifiableList().removeAt(index)

    override fun indexOf(element: E): Int = wrapper.get()?.indexOf(element) ?: -1

    override fun lastIndexOf(element: E): Int = wrapper.get()?.lastIndexOf(element) ?: -1

    override fun listIterator(): MutableListIterator<E> = if (!wrapper.isWriteable) {
        ArrayList(wrapper.get()?.toList() ?: emptyList()).listIterator()
    } else {
        getModifiableList().listIterator()
    }

    override fun listIterator(index: Int): MutableListIterator<E> = if (!wrapper.isWriteable) {
        ArrayList(wrapper.get()?.toList() ?: emptyList()).listIterator(index)
    } else {
        getModifiableList().listIterator(index)
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<E> = if (!wrapper.isWriteable) {
        ArrayList(wrapper.get()?.subList(fromIndex, toIndex) ?: throw IndexOutOfBoundsException("List is null"))
    } else {
        getModifiableList().subList(fromIndex, toIndex)
    }

    companion object {
        fun <E> of(name: String, defaultValue: List<E>? = null): FieldWrapperList<E> =
            FieldWrapperList(name, defaultValue)
    }
} 