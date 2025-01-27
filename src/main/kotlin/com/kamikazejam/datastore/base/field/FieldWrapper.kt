@file:Suppress("unused")

package com.kamikazejam.datastore.base.field

import com.google.common.base.Preconditions
import com.kamikazejam.datastore.base.Store
import com.kamikazejam.datastore.base.data.StoreData
import org.jetbrains.annotations.ApiStatus
import java.util.*

sealed interface FieldWrapper<D : StoreData<Any>> : FieldProvider {
    val name: String
    val isWriteable: Boolean
    // we need a creator function that can create an instance of D
    val creator: () -> D
    @set:ApiStatus.Internal
    var parent: Store<*, *>?

    fun getData(): D?

    fun setDataNotNull(data: D)

    override val fieldWrapper: FieldWrapper<*>
        get() = this

    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
}

sealed interface RequiredField<D : StoreData<Any>> : FieldWrapper<D> {
    val defaultValue: D
    fun setData(data: D)
    override fun setDataNotNull(data: D) = setData(data)

    override fun getData(): D

    companion object {
        @JvmStatic
        fun <D : StoreData<Any>> of(name: String, defaultValue: D, creator: () -> D = { defaultValue }): RequiredField<D> =
            RequiredFieldImpl(name, defaultValue, creator)
    }
}

sealed interface OptionalField<D : StoreData<Any>> : FieldWrapper<D> {
    val defaultValue: D?
    fun getDataOrDefault(default: D): D
    fun setData(data: D?)
    override fun setDataNotNull(data: D) = setData(data)

    override fun getData(): D?

    companion object {
        @JvmStatic
        fun <D : StoreData<Any>> of(name: String, defaultValue: D?, creator: () -> D): OptionalField<D> =
            OptionalFieldImpl(name, defaultValue, creator)
    }
}

@Suppress("DuplicatedCode")
private class RequiredFieldImpl<D : StoreData<Any>>(
    override val name: String,
    override val defaultValue: D,
    override val creator: () -> D = { defaultValue }
) : RequiredField<D> {
    private var data: D = defaultValue
    override var parent: Store<*, *>? = null
        set(value) {
            field = value
            data.setParent(value)
        }

    override fun getData(): D {
        Preconditions.checkState(parent != null, "[RequiredField#get] Field not registered with a parent document")
        Preconditions.checkState(data.parent != null, "[RequiredField#get] Data not registered with a parent document")
        return data
    }

    override fun setData(data: D) {
        Preconditions.checkState(isWriteable, "Cannot modify field '$name' in read-only mode")
        Preconditions.checkState(
            defaultValue::class.isInstance(data),
            "Data $data (${data.let { it::class.java }}) is not an instance of the field type (${defaultValue::class.java})"
        )
        this.data = data
        this.data.setParent(parent)
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
        if (other !is RequiredFieldImpl<*>) return false
        return data == other.data && name == other.name
    }

    override fun hashCode(): Int {
        return Objects.hash(data, name)
    }
}

private class OptionalFieldImpl<D : StoreData<Any>>(
    override val name: String,
    override val defaultValue: D?,
    override val creator: () -> D
) : OptionalField<D> {
    private var data: D? = defaultValue
    override var parent: Store<*, *>? = null
        set(value) {
            field = value
            data?.setParent(value)
        }

    override fun getData(): D? {
        val d = this.data
        Preconditions.checkState(parent != null, "[OptionalField#get] Field not registered with a parent document")
        Preconditions.checkState((d == null || d.parent != null), "[OptionalField#get] Data not registered with a parent document")
        return d
    }

    override fun getDataOrDefault(default: D): D {
        return getData() ?: default
    }

    override fun setData(data: D?) {
        Preconditions.checkState(isWriteable, "Cannot modify field '$name' in read-only mode")
        this.data = data
        this.data?.setParent(parent)
    }

    override val isWriteable: Boolean
        get() {
            val p = this.parent
            Preconditions.checkState(
                p != null,
                "[OptionalField#isWriteable] Field '$name' not registered with a parent document"
            )
            checkNotNull(p)
            return !p.readOnly && (data?.isWriteable ?: true)
        }

    override fun equals(other: Any?): Boolean {
        if (other !is OptionalFieldImpl<*>) return false
        return data == other.data && name == other.name
    }

    override fun hashCode(): Int {
        return Objects.hash(data, name)
    }
}