package com.kamikazejam.datastore.base.field

import com.kamikazejam.datastore.base.Store
import org.jetbrains.annotations.ApiStatus
import java.util.*

sealed interface FieldWrapper<T> : FieldProvider {
    val name: String
    val valueType: Class<*>
    val elementType: Class<*>?
    val isWriteable: Boolean
    
    @ApiStatus.Internal
    fun setParent(parent: Store<*, *>)
    
    fun getFieldType(): Class<T>
    
    fun getNullable(): T?
    
    fun setNotNull(value: T)
    
    override val fieldWrapper: FieldWrapper<*>
        get() = this
}

sealed interface RequiredField<T> : FieldWrapper<T> {
    val defaultValue: T
    fun get(): T
    fun set(value: T)
    
    override fun getNullable(): T = get()
    
    override fun setNotNull(value: T) = set(value)
    
    companion object {
        @JvmStatic
        fun <T> of(name: String, defaultValue: T, valueType: Class<T>): RequiredField<T> =
            RequiredFieldImpl(name, defaultValue, valueType)
    }
}

sealed interface OptionalField<T> : FieldWrapper<T> {
    val defaultValue: T?
    fun get(): T?
    fun getOrDefault(default: T): T
    fun set(value: T?)
    
    override fun getNullable(): T? = get()
    
    override fun setNotNull(value: T) = set(value)
    
    companion object {
        @JvmStatic
        fun <T> of(name: String, defaultValue: T?, valueType: Class<T>): OptionalField<T> =
            OptionalFieldImpl(name, defaultValue, valueType)
    }
}

private class RequiredFieldImpl<T>(
    override val name: String,
    override val defaultValue: T,
    override val valueType: Class<*>,
    override val elementType: Class<*>? = null
) : RequiredField<T> {
    private var value: T = defaultValue
    private var parent: Store<*, *>? = null

    override fun getFieldType(): Class<T> {
        @Suppress("UNCHECKED_CAST")
        return valueType as Class<T>
    }

    @ApiStatus.Internal
    override fun setParent(parent: Store<*, *>) {
        this.parent = parent
    }

    override fun get(): T {
        checkNotNull(parent) { "[RequiredField#get] Field not registered with a parent document" }
        return value
    }

    override fun set(value: T) {
        check(isWriteable) { "Cannot modify field '$name' in read-only mode" }
        this.value = value
    }

    override val isWriteable: Boolean
        get() {
            val p = this.parent
            checkNotNull(p) { "[RequiredField#isWriteable] Field not registered with a parent document" }
            return !p.readOnly
        }

    override fun equals(other: Any?): Boolean {
        if (other !is RequiredField<*>) return false
        return value == other.get() && name == other.name && valueType == other.valueType
    }

    override fun hashCode(): Int {
        return Objects.hash(value, name, valueType)
    }
}

private class OptionalFieldImpl<T>(
    override val name: String,
    override val defaultValue: T?,
    override val valueType: Class<*>,
    override val elementType: Class<*>? = null
) : OptionalField<T> {
    private var value: T? = defaultValue
    private var parent: Store<*, *>? = null

    override fun getFieldType(): Class<T> {
        @Suppress("UNCHECKED_CAST")
        return valueType as Class<T>
    }

    @ApiStatus.Internal
    override fun setParent(parent: Store<*, *>) {
        this.parent = parent
    }

    override fun get(): T? {
        checkNotNull(parent) { "[OptionalField#get] Field not registered with a parent document" }
        return value
    }

    override fun getOrDefault(default: T): T {
        return get() ?: default
    }

    override fun set(value: T?) {
        check(isWriteable) { "Cannot modify field '$name' in read-only mode" }
        this.value = value
    }

    override val isWriteable: Boolean
        get() {
            val p = this.parent
            checkNotNull(p) { "[OptionalField#isWriteable] Field not registered with a parent document" }
            return !p.readOnly
        }

    override fun equals(other: Any?): Boolean {
        if (other !is OptionalField<*>) return false
        return value == other.get() && name == other.name && valueType == other.valueType
    }

    override fun hashCode(): Int {
        return Objects.hash(value, name, valueType)
    }
}