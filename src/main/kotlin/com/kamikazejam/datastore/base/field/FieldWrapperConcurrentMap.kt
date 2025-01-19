@file:Suppress("UNCHECKED_CAST")

package com.kamikazejam.datastore.base.field

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

interface FieldWrapperConcurrentMap<K : Any, V : Any> : ConcurrentMap<K, V>, FieldProvider {
    val keyType: Class<K>
    val valueType: Class<V>
    
    companion object {
        @JvmStatic
        fun <K : Any, V : Any> of(name: String, defaultValue: ConcurrentMap<K, V> = ConcurrentHashMap(), keyType: Class<K>, valueType: Class<V>): FieldWrapperConcurrentMap<K, V> =
            FieldWrapperConcurrentMapImpl(name, defaultValue, keyType, valueType)
    }
}

private class FieldWrapperConcurrentMapImpl<K : Any, V : Any>(
    name: String,
    defaultValue: ConcurrentMap<K, V>,
    override val keyType: Class<K>,
    override val valueType: Class<V>
) : FieldWrapperConcurrentMap<K, V> {
    private var wrapper = RequiredField.of(
        name,
        ConcurrentHashMap(defaultValue),
        ConcurrentHashMap::class.java as Class<ConcurrentHashMap<K, V>>
    )

    override val fieldWrapper: FieldWrapper<*>
        get() = wrapper

    private fun getModifiableMap(): ConcurrentMap<K, V> {
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

    override fun putIfAbsent(key: K, value: V): V? = getModifiableMap().putIfAbsent(key, value)

    override fun remove(key: K, value: V): Boolean = getModifiableMap().remove(key, value)

    override fun replace(key: K, oldValue: V, newValue: V): Boolean =
        getModifiableMap().replace(key, oldValue, newValue)

    override fun replace(key: K, value: V): V? = getModifiableMap().replace(key, value)
} 