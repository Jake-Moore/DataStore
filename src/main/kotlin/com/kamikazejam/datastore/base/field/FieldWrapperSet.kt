package com.kamikazejam.datastore.base.field

@Suppress("unused")
class FieldWrapperSet<E> private constructor(
    name: String,
    defaultValue: Set<E>?
) : MutableSet<E>, FieldProvider {
    private var wrapper = FieldWrapper.ofColl(
        name,
        defaultValue?.let { HashSet(it) } ?: HashSet(),
        Set::class.java
    )

    override val fieldWrapper: FieldWrapper<*>
        get() = wrapper

    private fun getModifiableSet(): MutableSet<E> {
        if (!wrapper.isWriteable) {
            throw IllegalStateException("Cannot modify set in read-only mode")
        }
        return wrapper.get() ?: HashSet<E>().also { wrapper.set(it) }
    }

    override val size: Int
        get() = wrapper.get()?.size ?: 0

    override fun isEmpty(): Boolean = wrapper.get()?.isEmpty() ?: true

    override fun contains(element: E): Boolean = wrapper.get()?.contains(element) ?: false

    override fun iterator(): MutableIterator<E> = if (!wrapper.isWriteable) {
        ArrayList(wrapper.get()?.toSet() ?: emptySet()).iterator()
    } else {
        getModifiableSet().iterator()
    }

    override fun add(element: E): Boolean = getModifiableSet().add(element)

    override fun remove(element: E): Boolean = getModifiableSet().remove(element)

    override fun containsAll(elements: Collection<E>): Boolean =
        wrapper.get()?.containsAll(elements) ?: false

    override fun addAll(elements: Collection<E>): Boolean = getModifiableSet().addAll(elements)

    override fun removeAll(elements: Collection<E>): Boolean = getModifiableSet().removeAll(elements.toSet())

    override fun retainAll(elements: Collection<E>): Boolean = getModifiableSet().retainAll(elements.toSet())

    override fun clear() {
        if (!wrapper.isWriteable) {
            throw IllegalStateException("Cannot modify set in read-only mode")
        }
        wrapper.get()?.clear()
    }

    companion object {
        fun <E> of(name: String, defaultValue: Set<E>? = null): FieldWrapperSet<E> =
            FieldWrapperSet(name, defaultValue)
    }
} 