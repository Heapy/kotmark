package io.heapy.kotmark.ext.gfm.strikethrough.internal

import io.heapy.kotmark.ext.gfm.strikethrough.Strikethrough
import io.heapy.kotmark.node.Node
import io.heapy.kotmark.renderer.NodeRenderer
import kotlin.reflect.KClass

abstract class StrikethroughNodeRenderer : NodeRenderer {
    final override val nodeTypes: Set<KClass<out Node>> = setOf(Strikethrough::class)
}
