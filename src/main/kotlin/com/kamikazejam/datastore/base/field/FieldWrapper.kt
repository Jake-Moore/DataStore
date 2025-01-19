package com.kamikazejam.datastore.base.field

import com.google.common.base.Preconditions
import com.kamikazejam.datastore.base.Store
import org.bukkit.Bukkit
import org.jetbrains.annotations.ApiStatus
import java.util.*

sealed interface FieldWrapper<T> : FieldProvider {
    val name: String
    val valueType: Class<T>
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

@Suppress("DuplicatedCode")
private class RequiredFieldImpl<T>(
    override val name: String,
    override val defaultValue: T,
    override val valueType: Class<T>,
) : RequiredField<T> {
    private var value: T = defaultValue
    private var parent: Store<*, *>? = null

    override fun getFieldType(): Class<T> {
        return valueType
    }

    @ApiStatus.Internal
    override fun setParent(parent: Store<*, *>) {
        this.parent = parent
    }

    override fun get(): T {
        Preconditions.checkState(parent != null, "[RequiredField#get] Field not registered with a parent document")
        return value
    }

    override fun set(value: T) {
        Preconditions.checkState(isWriteable, "Cannot modify field '$name' in read-only mode")
        Preconditions.checkState(
            value == null || defaultValue!!::class.isInstance(value),
            "Value $value (${value?.let { it::class.java }}) is not an instance of the field type (${defaultValue!!::class.java})"
        )
        Bukkit.getLogger().info("Setting required field ($name) value to $value (${value?.let { it::class.java }})")
        this.value = value
    }

    override val isWriteable: Boolean
        get() {
            val p = this.parent
            Preconditions.checkState(p != null, "[RequiredField#isWriteable] Field not registered with a parent document")
            checkNotNull(p)
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
    override val valueType: Class<T>,
) : OptionalField<T> {
    private var value: T? = defaultValue
    private var parent: Store<*, *>? = null

    override fun getFieldType(): Class<T> {
        return valueType
    }

    @ApiStatus.Internal
    override fun setParent(parent: Store<*, *>) {
        this.parent = parent
    }

    override fun get(): T? {
        Preconditions.checkState(
            parent != null,
            "[OptionalField#get] Field not registered with a parent document"
        )
        return value
    }

    override fun getOrDefault(default: T): T {
        return get() ?: default
    }

    override fun set(value: T?) {
        Preconditions.checkState(isWriteable, "Cannot modify field '$name' in read-only mode")
        Preconditions.checkState(
            value == null || valueType.isInstance(value) || valueType::class.java.isInstance(value),
            "Value $value (${value?.let { it::class.java }}) is not an instance of the field type (${valueType::class.java})"
        )
        Bukkit.getLogger().info("Setting optional field ($name) value to $value (${value?.let { it::class.java }})")
        this.value = value
    }

    override val isWriteable: Boolean
        get() {
            val p = this.parent
            Preconditions.checkState(p != null, "[OptionalField#isWriteable] Field not registered with a parent document")
            checkNotNull(p)
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