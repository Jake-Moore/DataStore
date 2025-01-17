package com.kamikazejam.datastore.base.field

@Suppress("unused")
class FieldWrapperMap<K, V> private constructor(
    name: String,
    defaultValue: Map<K, V>?
) : MutableMap<K, V>, FieldProvider {
    private var wrapper = FieldWrapper.ofMap(
        name,
        defaultValue?.let { HashMap(it) } ?: HashMap(),
        Map::class.java
    )

    override val fieldWrapper: FieldWrapper<*>
        get() = wrapper

    private fun getModifiableMap(): MutableMap<K, V> {
        if (!wrapper.isWriteable) {
            throw IllegalStateException("Cannot modify map in read-only mode")
        }
        return wrapper.get() ?: HashMap<K, V>().also { wrapper.set(it) }
    }

    override val size: Int
        get() = wrapper.get()?.size ?: 0

    override fun isEmpty(): Boolean = wrapper.get()?.isEmpty() ?: true

    override fun containsKey(key: K): Boolean = wrapper.get()?.containsKey(key) ?: false

    override fun containsValue(value: V): Boolean = wrapper.get()?.containsValue(value) ?: false

    override fun get(key: K): V? = wrapper.get()?.get(key)

    override fun put(key: K, value: V): V? = getModifiableMap().put(key, value)

    override fun remove(key: K): V? = getModifiableMap().remove(key)

    override fun putAll(from: Map<out K, V>) = getModifiableMap().putAll(from)

    override fun clear() {
        if (!wrapper.isWriteable) {
            throw IllegalStateException("Cannot modify map in read-only mode")
        }
        wrapper.get()?.clear()
    }

    override val keys: MutableSet<K>
        get() = if (!wrapper.isWriteable) {
            HashSet(wrapper.get()?.keys ?: emptySet())
        } else {
            getModifiableMap().keys
        }

    override val values: MutableCollection<V>
        get() = if (!wrapper.isWriteable) {
            ArrayList(wrapper.get()?.values ?: emptyList())
        } else {
            getModifiableMap().values
        }

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = if (!wrapper.isWriteable) {
            HashSet(wrapper.get()?.entries ?: emptySet())
        } else {
            getModifiableMap().entries
        }

    companion object {
        @JvmStatic
        fun <K, V> of(name: String, defaultValue: Map<K, V>? = null): FieldWrapperMap<K, V> =
            FieldWrapperMap(name, defaultValue)
    }
} 