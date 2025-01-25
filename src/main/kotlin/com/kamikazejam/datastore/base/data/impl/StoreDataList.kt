package com.kamikazejam.datastore.base.data.impl

import com.google.common.base.Preconditions
import com.kamikazejam.datastore.base.Store
import com.kamikazejam.datastore.base.data.SimpleStoreData
import com.kamikazejam.datastore.base.data.StoreData
import com.kamikazejam.datastore.util.JacksonUtil
import org.bson.Document

@Suppress("unused")
class StoreDataList<T : StoreData<*>>(
    private val elementCreator: () -> T,
    initialElements: List<T> = emptyList()
) : SimpleStoreData<List<T>>(initialElements.toMutableList()), MutableList<T> {

    private val internalList: MutableList<T>
        get() = get() as MutableList<T>


    // ------------------------------------------------------------ //
    //                         SimpleStoreData                      //
    // ------------------------------------------------------------ //
    override fun setParent(parent: Store<*, *>?) {
        super.setParent(parent)
        // Propagate parent to all elements
        internalList.forEach { it.setParent(parent) }
    }

    override fun serializeToBSON(): Any {
        val doc = Document()
        doc["size"] = internalList.size

        // Insert each StoreData element into its index (using JacksonUtil serialization)
        for ((i, data) in internalList.withIndex()) {
            JacksonUtil.serializeDataIntoDocumentKey(i.toString(), data, doc)
        }
        
        return doc
    }

    override fun deserializeFromBSON(bson: Document, key: String) {
        val size = bson.getInteger("size", 0)
        internalList.clear()
        
        // Read each element by its index
        for (i in 0 until size) {
            val data = JacksonUtil.deserializeIntoStoreData(i.toString(), bson, parent, elementCreator)
                ?: throw IllegalStateException("Failed to deserialize element at index $i")
            internalList.add(data)
        }
    }

    // ------------------------------------------------------------ //
    //                       MutableList Methods                    //
    // ------------------------------------------------------------ //
    override fun add(element: T): Boolean {
        Preconditions.checkState(isWriteable, "Cannot modify list in read-only mode")
        element.setParent(parent)
        return internalList.add(element)
    }

    override fun addAll(elements: Collection<T>): Boolean {
        Preconditions.checkState(isWriteable, "Cannot modify list in read-only mode")
        elements.forEach { it.setParent(parent) }
        return internalList.addAll(elements)
    }

    override fun remove(element: T): Boolean {
        Preconditions.checkState(isWriteable, "Cannot modify list in read-only mode")
        return internalList.remove(element)
    }

    override fun clear() {
        Preconditions.checkState(isWriteable, "Cannot modify list in read-only mode")
        internalList.clear()
    }

    override val size: Int
        get() = internalList.size

    override fun isEmpty(): Boolean = internalList.isEmpty()

    override fun contains(element: T): Boolean = internalList.contains(element)

    override fun containsAll(elements: Collection<T>): Boolean = internalList.containsAll(elements)

    override fun indexOf(element: T): Int = internalList.indexOf(element)

    override fun lastIndexOf(element: T): Int = internalList.lastIndexOf(element)

    override fun addAll(index: Int, elements: Collection<T>): Boolean {
        Preconditions.checkState(isWriteable, "Cannot modify list in read-only mode")
        elements.forEach { it.setParent(parent) }
        return internalList.addAll(index, elements)
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        Preconditions.checkState(isWriteable, "Cannot modify list in read-only mode")
        return internalList.removeAll(elements.toSet())
    }

    override fun retainAll(elements: Collection<T>): Boolean {
        Preconditions.checkState(isWriteable, "Cannot modify list in read-only mode")
        return internalList.retainAll(elements.toSet())
    }

    override fun add(index: Int, element: T) {
        Preconditions.checkState(isWriteable, "Cannot modify list in read-only mode")
        element.setParent(parent)
        internalList.add(index, element)
    }

    override fun removeAt(index: Int): T {
        Preconditions.checkState(isWriteable, "Cannot modify list in read-only mode")
        return internalList.removeAt(index)
    }

    override fun iterator(): MutableIterator<T> = internalList.iterator()

    override fun listIterator(): MutableListIterator<T> = internalList.listIterator()

    override fun listIterator(index: Int): MutableListIterator<T> = internalList.listIterator(index)

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> = internalList.subList(fromIndex, toIndex)

    override operator fun get(index: Int): T = internalList[index]

    override operator fun set(index: Int, element: T): T {
        Preconditions.checkState(isWriteable, "Cannot modify list in read-only mode")
        element.setParent(parent)
        return internalList.set(index, element)
    }

    companion object {
        @JvmStatic
        fun <T : StoreData<*>> of(elementCreator: () -> T): StoreDataList<T> {
            return StoreDataList(elementCreator)
        }

        @JvmStatic
        fun <T : StoreData<*>> of(elementCreator: () -> T, initialElements: List<T>): StoreDataList<T> {
            return StoreDataList(elementCreator, initialElements)
        }
    }
} 