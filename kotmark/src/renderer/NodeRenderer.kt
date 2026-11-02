package io.heapy.kotmark.renderer

import io.heapy.kotmark.node.Node
import kotlin.reflect.KClass

interface NodeRenderer {
    val nodeTypes: Set<KClass<out Node>>

    fun render(node: Node)

    fun beforeRoot(rootNode: Node) = Unit

    fun afterRoot(rootNode: Node) = Unit
}
