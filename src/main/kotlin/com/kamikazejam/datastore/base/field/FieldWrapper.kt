package com.kamikazejam.datastore.base.field

import com.google.common.base.Preconditions
import com.kamikazejam.datastore.base.Store
import com.kamikazejam.datastore.base.data.StoreData
import org.jetbrains.annotations.ApiStatus
import java.util.*

sealed interface FieldWrapper<T : Any> : FieldProvider {
    val name: String
    val isWriteable: Boolean
    
    @ApiStatus.Internal
    fun setParent(parent: Store<*, *>)

    fun getDataType(): Class<T>

    fun getNullable(): T?

    override val fieldWrapper: FieldWrapper<*>
        get() = this
}

sealed interface RequiredField<T : Any> : FieldWrapper<T> {
    val defaultValue: StoreData<T>
    fun get(): T
    fun set(value: T)
    fun set(data: StoreData<T>)

    override fun getNullable(): T = get()

    companion object {
        @JvmStatic
        fun <T : Any> of(name: String, defaultValue: StoreData<T>): RequiredField<T> = RequiredFieldImpl(name, defaultValue)
    }
}

sealed interface OptionalField<T : Any> : FieldWrapper<T> {
    val defaultValue: StoreData<T>?
    fun get(): T?
    fun getOrDefault(default: T): T
    fun set(data: StoreData<T>?)

    override fun getNullable(): T? = get()

    companion object {
        @JvmStatic
        fun <T : Any> of(name: String, defaultValue: StoreData<T>?, dataType: Class<T>): OptionalField<T> =
            OptionalFieldImpl(name, defaultValue, dataType)
    }
}

@Suppress("DuplicatedCode")
private class RequiredFieldImpl<T : Any>(
    override val name: String,
    override val defaultValue: StoreData<T>,
) : RequiredField<T> {
    private var data: StoreData<T> = defaultValue
    private var parent: Store<*, *>? = null

    override fun getDataType(): Class<T> {
        return data.dataType
    }

    @ApiStatus.Internal
    override fun setParent(parent: Store<*, *>) {
        this.parent = parent
    }

    override fun get(): T {
        Preconditions.checkState(parent != null, "[RequiredField#get] Field not registered with a parent document")
        return data.value
    }

    override fun set(value: T) {
        Preconditions.checkState(isWriteable, "Cannot modify field '$name' in read-only mode")
        Preconditions.checkState(
            defaultValue.value::class.isInstance(value),
            "Value $value (${value.let { it::class.java }}) is not an instance of the field type (${defaultValue::class.java})"
        )
        data.value = value
    }

    override fun set(data: StoreData<T>) {
        this.set(data.value)
    }

    override val isWriteable: Boolean
        get() {
            val p = this.parent
            Preconditions.checkState(p != null, "[RequiredField#isWriteable] Field not registered with a parent document")
            checkNotNull(p)
            return !p.readOnly
        }

    override fun equals(other: Any?): Boolean {
        if (other !is RequiredFieldImpl<*>) return false
        return data.value == other.data.value && name == other.name && data.dataType == other.data.dataType
    }

    override fun hashCode(): Int {
        return Objects.hash(data.value, name, data.dataType)
    }
}

private class OptionalFieldImpl<T : Any>(
    override val name: String,
    override val defaultValue: StoreData<T>?,
    val dateType: Class<T>,
) : OptionalField<T> {
    private var data: StoreData<T>? = defaultValue
    private var parent: Store<*, *>? = null

    override fun getDataType(): Class<T> {
        return dateType
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
        return data?.value
    }

    override fun getOrDefault(default: T): T {
        return get() ?: default
    }

    override fun set(data: StoreData<T>?) {
        Preconditions.checkState(isWriteable, "Cannot modify field '$name' in read-only mode")
        Preconditions.checkState(
            data == null || dateType.isInstance(data) || dateType::class.java.isInstance(data),
            "Value $data (${data?.let { it::class.java }}) is not an instance of the field type (${dateType::class.java})"
        )
        this.data = data
    }

    override val isWriteable: Boolean
        get() {
            val p = this.parent
            Preconditions.checkState(p != null, "[OptionalField#isWriteable] Field not registered with a parent document")
            checkNotNull(p)
            return !p.readOnly
        }

    override fun equals(other: Any?): Boolean {
        if (other !is OptionalFieldImpl<*>) return false
        return data?.value == other.data?.value && name == other.name && dateType == other.dateType
    }

    override fun hashCode(): Int {
        return Objects.hash(data?.value, name, dateType)
    }
}