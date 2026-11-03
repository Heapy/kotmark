package io.heapy.kotmark.renderer

import io.heapy.kotmark.node.Node
import kotlin.reflect.KClass

interface NodeRenderer {
    val nodeTypes: Set<KClass<out Node>>

    fun render(node: Node)

    fun beforeRoot(rootNode: Node) = Unit

    fun afterRoot(rootNode: Node) = Unit
}

/**
 * Creates a renderer for exactly [N], with a typed handler and no lifecycle hooks.
 * Use this inside a renderer factory when the handler needs state local to one render.
 * Calling the returned renderer directly with another node type is a programming error.
 */
inline fun <reified N : Node> typedNodeRenderer(crossinline block: (N) -> Unit): NodeRenderer =
    object : NodeRenderer {
        override val nodeTypes: Set<KClass<out Node>> = setOf(N::class)

        override fun render(node: Node) {
            block(node as N)
        }
    }
