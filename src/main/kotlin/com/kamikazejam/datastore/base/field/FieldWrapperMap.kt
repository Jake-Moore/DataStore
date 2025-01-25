@file:Suppress("UNCHECKED_CAST")

package com.kamikazejam.datastore.base.field

interface FieldWrapperMap<K, V> : MutableMap<K, V>, FieldProvider {
    val keyType: Class<K>
    val valueType: Class<V>
    
    companion object {
        @JvmStatic
        fun <K, V> of(name: String, keyType: Class<K>, valueType: Class<V>, defaultValue: Map<K, V> = mutableMapOf()): FieldWrapperMap<K, V> =
            FieldWrapperMapImpl(name, defaultValue, keyType, valueType)
    }
}

private class FieldWrapperMapImpl<K, V>(
    name: String,
    defaultValue: Map<K, V>,
    override val keyType: Class<K>,
    override val valueType: Class<V>
) : FieldWrapperMap<K, V> {
    private var wrapper = RequiredField.of(
        name,
        defaultValue.let { HashMap(it) }
    )

    override val fieldWrapper: FieldWrapper<*>
        get() = wrapper

    private fun getModifiableMap(): MutableMap<K, V> {
        if (!wrapper.isWriteable) {
            throw IllegalStateException("Cannot modify map in read-only mode")
        }
        return wrapper.get()
    }

    override val size: Int
        get() = wrapper.get().size

    override fun isEmpty(): Boolean = wrapper.get().isEmpty()

    override fun containsKey(key: K): Boolean = wrapper.get().containsKey(key)

    override fun containsValue(value: V): Boolean = wrapper.get().containsValue(value)

    override fun get(key: K): V? = wrapper.get()[key]

    override fun put(key: K, value: V): V? = getModifiableMap().put(key, value)

    override fun remove(key: K): V? = getModifiableMap().remove(key)

    override fun putAll(from: Map<out K, V>) = getModifiableMap().putAll(from)

    override fun clear() {
        if (!wrapper.isWriteable) {
            throw IllegalStateException("Cannot modify map in read-only mode")
        }
        wrapper.get().clear()
    }

    override val keys: MutableSet<K>
        get() = if (!wrapper.isWriteable) {
            HashSet(wrapper.get().keys)
        } else {
            getModifiableMap().keys
        }

    override val values: MutableCollection<V>
        get() = if (!wrapper.isWriteable) {
            ArrayList(wrapper.get().values)
        } else {
            getModifiableMap().values
        }

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = if (!wrapper.isWriteable) {
            HashSet(wrapper.get().entries)
        } else {
            getModifiableMap().entries
        }
} 