package com.kamikazejam.datastore.connections.storage.iterator

import java.util.function.Function

/**
 * The transformer is slightly more complicated because we allow the transformer to return a null value.
 * When a null value is supplied, the iterable skips that element and continues to the next one.
 * As a result, we pre-compute the next element as needed in order to maintain a valid state.
 */
class TransformingIterator<S, T>(
    private val sourceIterator: Iterator<S>,
    private val transformer: Function<S, T?>
) : Iterator<T> {
    private var nextElement: T? = null
    private var hasNextElement = false

    init {
        advanceToNext() // Initialize the first valid element
    }

    private fun advanceToNext() {
        while (sourceIterator.hasNext()) {
            val sourceElement = sourceIterator.next()
            nextElement = transformer.apply(sourceElement)
            if (nextElement != null) {
                hasNextElement = true
                return
            }
        }
        // No more valid elements
        hasNextElement = false
    }

    override fun hasNext(): Boolean {
        return hasNextElement
    }

    override fun next(): T {
        val result = nextElement
        if (!hasNextElement || result == null) {
            throw NoSuchElementException()
        }
        advanceToNext() // Prepare for the next call
        return result
    }
}

