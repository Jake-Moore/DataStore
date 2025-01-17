package com.kamikazejam.datastore.base.field

import com.kamikazejam.datastore.base.Store
import org.jetbrains.annotations.ApiStatus
import java.util.*

/**
 * A wrapper for fields that enforces access control based on Store state
 */
@Suppress("unused", "UNCHECKED_CAST")
class FieldWrapper<T> private constructor(
    val name: String,
    private var value: T?,
    private val valueType: Class<*>,
    // We don't actually need `elementType` in our implementation yet, but we
    //  keep it in case it is needed in the future
    private val elementType: Class<*>?
) : FieldProvider {

    val defaultValue = value
    private var parent: Store<*, *>? = null

    private constructor(name: String, defaultValue: T?, valueType: Class<T>) : this(name, defaultValue, valueType, null)

    override val fieldWrapper: FieldWrapper<*>
        get() = this

    fun getValueType(): Class<T> {
        return valueType as Class<T>
    }

    @ApiStatus.Internal
    fun setParent(parent: Store<*, *>) {
        this.parent = parent
    }

    fun get(): T? {
        checkNotNull(parent) { "[FieldWrapper#get] Field not registered with a parent document" }
        return value
    }

    fun set(value: T?) {
        check(isWriteable) { "Cannot modify field '$name' in read-only mode" }
        this.value = value
    }

    val isWriteable: Boolean
        get() {
            val p = this.parent
            checkNotNull(p) { "[FieldWrapper#isWriteable] Field not registered with a parent document" }
            return !p.readOnly
        }

    override fun equals(other: Any?): Boolean {
        if (other !is FieldWrapper<*>) return false
        return value == other.value && name == other.name && valueType == other.valueType
    }

    override fun hashCode(): Int {
        return Objects.hash(value, name, valueType)
    }

    override fun toString(): String {
        return "FieldWrapper{name='$name', value=$value, valueType=$valueType}"
    }

    companion object {
        // ------------------------------------------------------ //
        // Static Constructors                                    //
        // ------------------------------------------------------ //
        @JvmStatic
        fun <T> of(name: String, defaultValue: T?, valueType: Class<T>): FieldWrapper<T> {
            return FieldWrapper(name, defaultValue, valueType)
        }

        // Generic constructor for any collection type
        fun <C : Collection<E>?, E> ofColl(
            name: String,
            defaultValue: C?,
            collectionType: Class<in C>
        ): FieldWrapper<C?> {
            return FieldWrapper(name, defaultValue, collectionType, null)
        }

        // Generic constructor for any map type
        fun <K, V, M : Map<K, V>?> ofMap(
            name: String,
            defaultValue: M?,
            mapType: Class<in M>
        ): FieldWrapper<M?> {
            return FieldWrapper(name, defaultValue, mapType, null)
        }
    }
}