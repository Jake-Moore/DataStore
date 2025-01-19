@file:Suppress("UNCHECKED_CAST")

package com.kamikazejam.datastore.base.field

interface FieldWrapperSet<E> : MutableSet<E>, FieldProvider {
    val elementType: Class<E>
    
    companion object {
        @JvmStatic
        fun <E> of(name: String, defaultValue: Set<E> = HashSet(), elementType: Class<E>): FieldWrapperSet<E> =
            FieldWrapperSetImpl(name, defaultValue, elementType)
    }
}

private class FieldWrapperSetImpl<E>(
    name: String,
    defaultValue: Set<E>,
    override val elementType: Class<E>
) : FieldWrapperSet<E> {
    private var wrapper = RequiredField.of(
        name,
        defaultValue.let { HashSet(it) },
        HashSet::class.java as Class<HashSet<E>>
    )

    override val fieldWrapper: FieldWrapper<*>
        get() = wrapper

    private fun getModifiableSet(): MutableSet<E> {
        if (!wrapper.isWriteable) {
            throw IllegalStateException("Cannot modify set in read-only mode")
        }
        return wrapper.get()
    }

    override val size: Int
        get() = wrapper.get().size

    override fun isEmpty(): Boolean = wrapper.get().isEmpty()

    override fun contains(element: E): Boolean = wrapper.get().contains(element)

    override fun iterator(): MutableIterator<E> = if (!wrapper.isWriteable) {
        ArrayList(wrapper.get().toSet()).iterator()
    } else {
        getModifiableSet().iterator()
    }

    override fun add(element: E): Boolean = getModifiableSet().add(element)

    override fun remove(element: E): Boolean = getModifiableSet().remove(element)

    override fun containsAll(elements: Collection<E>): Boolean = wrapper.get().containsAll(elements)

    override fun addAll(elements: Collection<E>): Boolean = getModifiableSet().addAll(elements)

    override fun removeAll(elements: Collection<E>): Boolean = getModifiableSet().removeAll(elements.toSet())

    override fun retainAll(elements: Collection<E>): Boolean = getModifiableSet().retainAll(elements.toSet())

    override fun clear() {
        if (!wrapper.isWriteable) {
            throw IllegalStateException("Cannot modify set in read-only mode")
        }
        wrapper.get().clear()
    }
} 