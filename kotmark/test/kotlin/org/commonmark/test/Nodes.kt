package org.commonmark.test

import org.commonmark.node.Node

/**
 * Test helpers for working with the children of a [Node]. Mirrors the `Nodes` helper from commonmark-java.
 */
object Nodes {
    fun getChildren(parent: Node): List<Node> {
        val children = mutableListOf<Node>()
        var child = parent.firstChild
        while (child != null) {
            children.add(child)
            child = child.next
        }
        return children
    }

    /**
     * Recursively try to find a node matching [predicate] within the children of [parent] (parent itself is not
     * checked).
     */
    fun tryFind(parent: Node, predicate: (Node) -> Boolean): Node? {
        var node = parent.firstChild
        while (node != null) {
            // Snapshot next before the caller can move the node around.
            val next = node.next
            if (predicate(node)) {
                return node
            }
            val result = tryFind(node, predicate)
            if (result != null) {
                return result
            }
            node = next
        }
        return null
    }

    inline fun <reified T : Node> tryFind(parent: Node): T? = tryFind(parent) { it is T } as T?

    /** Like [tryFind], but throws if no matching node is found. */
    inline fun <reified T : Node> find(parent: Node): T =
        requireNotNull(tryFind<T>(parent)) { "Could not find a ${T::class.simpleName} node in $parent" }
}
