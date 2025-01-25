@file:Suppress("unused")

package com.kamikazejam.datastore.base.field

import com.google.common.base.Preconditions
import com.kamikazejam.datastore.base.Store
import com.kamikazejam.datastore.base.data.StoreData
import org.jetbrains.annotations.ApiStatus
import java.util.*

sealed interface FieldWrapper<T : Any, D : StoreData<T>> : FieldProvider {
    val name: String
    val isWriteable: Boolean

    @ApiStatus.Internal
    fun setParent(parent: Store<*, *>)

    fun getDataType(): Class<T>

    fun getNullable(): D?

    override val fieldWrapper: FieldWrapper<*,*>
        get() = this
}

sealed interface RequiredField<T : Any, D : StoreData<T>> : FieldWrapper<T, D> {
    val defaultValue: D
    fun get(): D
    fun set(data: D)

    override fun getNullable(): D = get()

    fun getData(): D

    companion object {
        @JvmStatic
        fun <T : Any, D : StoreData<T>> of(name: String, defaultValue: D): RequiredField<T, D> =
            RequiredFieldImpl(name, defaultValue)
    }
}

sealed interface OptionalField<T : Any, D : StoreData<T>> : FieldWrapper<T, D> {
    val defaultValue: StoreData<T>?
    fun get(): D?
    fun getOrDefault(default: D): D
    fun set(data: D?)

    override fun getNullable(): D? = get()

    fun getData(): D?

    companion object {
        @JvmStatic
        fun <T : Any, D : StoreData<T>> of(name: String, defaultValue: D?, dataType: Class<T>): OptionalField<T, D> =
            OptionalFieldImpl(name, defaultValue, dataType)
    }
}

@Suppress("DuplicatedCode")
private class RequiredFieldImpl<T : Any, D : StoreData<T>>(
    override val name: String,
    override val defaultValue: D,
) : RequiredField<T, D> {
    private var data: D = defaultValue
    private var parent: Store<*, *>? = null

    override fun getData(): D {
        return data
    }

    override fun getDataType(): Class<T> {
        return data.dataType
    }

    @ApiStatus.Internal
    override fun setParent(parent: Store<*, *>) {
        this.parent = parent
    }

    override fun get(): D {
        Preconditions.checkState(parent != null, "[RequiredField#get] Field not registered with a parent document")
        return data
    }

    override fun set(data: D) {
        Preconditions.checkState(isWriteable, "Cannot modify field '$name' in read-only mode")
        Preconditions.checkState(
            defaultValue::class.isInstance(data),
            "Data $data (${data.let { it::class.java }}) is not an instance of the field type (${defaultValue::class.java})"
        )
        this.data = data
    }

    override val isWriteable: Boolean
        get() {
            val p = this.parent
            Preconditions.checkState(
                p != null,
                "[RequiredField#isWriteable] Field not registered with a parent document"
            )
            checkNotNull(p)
            return !p.readOnly
        }

    override fun equals(other: Any?): Boolean {
        if (other !is RequiredFieldImpl<*,*>) return false
        return data == other.data && name == other.name && data.dataType == other.data.dataType
    }

    override fun hashCode(): Int {
        return Objects.hash(data, name, data.dataType)
    }
}

private class OptionalFieldImpl<T : Any, D : StoreData<T>>(
    override val name: String,
    override val defaultValue: D?,
    val dateType: Class<T>,
) : OptionalField<T, D> {
    private var data: D? = defaultValue
    private var parent: Store<*, *>? = null

    override fun getData(): D? {
        return data
    }

    override fun getDataType(): Class<T> {
        return dateType
    }

    @ApiStatus.Internal
    override fun setParent(parent: Store<*, *>) {
        this.parent = parent
    }

    override fun get(): D? {
        Preconditions.checkState(
            parent != null,
            "[OptionalField#get] Field not registered with a parent document"
        )
        return data
    }

    override fun getOrDefault(default: D): D {
        return get() ?: default
    }

    override fun set(data: D?) {
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
            Preconditions.checkState(
                p != null,
                "[OptionalField#isWriteable] Field not registered with a parent document"
            )
            checkNotNull(p)
            return !p.readOnly
        }

    override fun equals(other: Any?): Boolean {
        if (other !is OptionalFieldImpl<*, *>) return false
        return data == other.data && name == other.name && dateType == other.dateType
    }

    override fun hashCode(): Int {
        return Objects.hash(data, name, dateType)
    }
}