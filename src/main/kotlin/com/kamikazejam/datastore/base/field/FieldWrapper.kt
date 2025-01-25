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
    // we need a creator function that can create an instance of D
    val creator: () -> D

    @ApiStatus.Internal
    fun setParent(parent: Store<*, *>)

    fun getData(): D?

    fun setDataNotNull(data: D)

    override val fieldWrapper: FieldWrapper<*,*>
        get() = this
}

sealed interface RequiredField<T : Any, D : StoreData<T>> : FieldWrapper<T, D> {
    val defaultValue: D
    fun setData(data: D)
    override fun setDataNotNull(data: D) = setData(data)

    override fun getData(): D

    companion object {
        @JvmStatic
        fun <T : Any, D : StoreData<T>> of(name: String, defaultValue: D, creator: () -> D = { defaultValue }): RequiredField<T, D> =
            RequiredFieldImpl(name, defaultValue, creator)
    }
}

sealed interface OptionalField<T : Any, D : StoreData<T>> : FieldWrapper<T, D> {
    val defaultValue: D?
    fun getDataOrDefault(default: D): D
    fun setData(data: D?)
    override fun setDataNotNull(data: D) = setData(data)

    override fun getData(): D?

    companion object {
        @JvmStatic
        fun <T : Any, D : StoreData<T>> of(name: String, defaultValue: D?, creator: () -> D): OptionalField<T, D> =
            OptionalFieldImpl(name, defaultValue, creator)
    }
}

@Suppress("DuplicatedCode")
private class RequiredFieldImpl<T : Any, D : StoreData<T>>(
    override val name: String,
    override val defaultValue: D,
    override val creator: () -> D = { defaultValue }
) : RequiredField<T, D> {
    private var data: D = defaultValue
    private var parent: Store<*, *>? = null

    override fun getData(): D {
        Preconditions.checkState(parent != null, "[RequiredField#get] Field not registered with a parent document")
        Preconditions.checkState(data.parent != null, "[RequiredField#get] Data not registered with a parent document")
        return data
    }

    @ApiStatus.Internal
    override fun setParent(parent: Store<*, *>) {
        this.parent = parent
        this.data.parent = parent
    }

    override fun setData(data: D) {
        Preconditions.checkState(isWriteable, "Cannot modify field '$name' in read-only mode")
        Preconditions.checkState(
            defaultValue::class.isInstance(data),
            "Data $data (${data.let { it::class.java }}) is not an instance of the field type (${defaultValue::class.java})"
        )
        this.data = data
        this.data.parent = parent
    }

    override val isWriteable: Boolean
        get() {
            val p = this.parent
            Preconditions.checkState(
                p != null,
                "[RequiredField#isWriteable] Field not registered with a parent document"
            )
            checkNotNull(p)
            return !p.readOnly && data.isWriteable
        }

    override fun equals(other: Any?): Boolean {
        if (other !is RequiredFieldImpl<*,*>) return false
        return data == other.data && name == other.name
    }

    override fun hashCode(): Int {
        return Objects.hash(data, name)
    }
}

private class OptionalFieldImpl<T : Any, D : StoreData<T>>(
    override val name: String,
    override val defaultValue: D?,
    override val creator: () -> D
) : OptionalField<T, D> {
    private var data: D? = defaultValue
    private var parent: Store<*, *>? = null

    override fun getData(): D? {
        val d = this.data
        Preconditions.checkState(parent != null, "[OptionalField#get] Field not registered with a parent document")
        Preconditions.checkState((d == null || d.parent != null), "[OptionalField#get] Data not registered with a parent document")
        return d
    }

    @ApiStatus.Internal
    override fun setParent(parent: Store<*, *>) {
        this.parent = parent
        this.data?.parent = parent
    }

    override fun getDataOrDefault(default: D): D {
        return getData() ?: default
    }

    override fun setData(data: D?) {
        Preconditions.checkState(isWriteable, "Cannot modify field '$name' in read-only mode")
        this.data = data
        this.data?.parent = parent
    }

    override val isWriteable: Boolean
        get() {
            val p = this.parent
            Preconditions.checkState(
                p != null,
                "[OptionalField#isWriteable] Field not registered with a parent document"
            )
            checkNotNull(p)
            return !p.readOnly && (data?.isWriteable ?: true)
        }

    override fun equals(other: Any?): Boolean {
        if (other !is OptionalFieldImpl<*, *>) return false
        return data == other.data && name == other.name
    }

    override fun hashCode(): Int {
        return Objects.hash(data, name)
    }
}