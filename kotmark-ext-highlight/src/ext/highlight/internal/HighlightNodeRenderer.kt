package io.heapy.kotmark.ext.highlight.internal

import io.heapy.kotmark.ext.highlight.Highlight
import io.heapy.kotmark.node.Node
import io.heapy.kotmark.renderer.NodeRenderer
import kotlin.reflect.KClass

abstract class HighlightNodeRenderer : NodeRenderer {
    final override val nodeTypes: Set<KClass<out Node>> = setOf(Highlight::class)
}
