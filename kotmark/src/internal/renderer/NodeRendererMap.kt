package io.heapy.kotmark.internal.renderer

import io.heapy.kotmark.node.Node
import io.heapy.kotmark.renderer.NodeRenderer
import kotlin.reflect.KClass

class NodeRendererMap {
    private val nodeRenderers = mutableListOf<NodeRenderer>()
    private val renderers = mutableMapOf<KClass<out Node>, NodeRenderer>()

    fun add(nodeRenderer: NodeRenderer) {
        nodeRenderers.add(nodeRenderer)
        for (nodeType in nodeRenderer.nodeTypes) {
            if (nodeType !in renderers) {
                renderers[nodeType] = nodeRenderer
            }
        }
    }

    fun render(node: Node) {
        renderers[node::class]?.render(node)
    }

    fun beforeRoot(node: Node) {
        nodeRenderers.forEach { it.beforeRoot(node) }
    }

    fun afterRoot(node: Node) {
        nodeRenderers.forEach { it.afterRoot(node) }
    }
}
