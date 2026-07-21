package org.commonmark.node

/**
 * Utility class for working with multiple [Node]s.
 */
object Nodes {
    /** The nodes between (not including) start and end. */
    fun between(start: Node, end: Node?): Iterable<Node> = iterable(start.next, end)
}

private fun iterable(first: Node?, end: Node?): Iterable<Node> =
    Iterable {
        object : Iterator<Node> {
            private var node = first

            override fun hasNext(): Boolean =
                node != null && node !== end

            override fun next(): Node {
                val result = node ?: throw NoSuchElementException()
                // Snapshot the next node before the caller can unlink or reparent the result.
                node = result.next
                return result
            }
        }
    }
