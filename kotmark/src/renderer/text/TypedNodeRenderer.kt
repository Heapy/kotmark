package io.heapy.kotmark.renderer.text

import io.heapy.kotmark.node.Node
import io.heapy.kotmark.renderer.typedNodeRenderer

/**
 * Registers a handler for exactly [N]; the first registration for a node type wins.
 * The receiver belongs to the current render. Captured mutable state in [block] is shared;
 * use [TextContentRenderer.Configuration.nodeRendererFactory] for state local to each render.
 */
inline fun <reified N : Node> TextContentRenderer.Configuration.nodeRenderer(
    crossinline block: TextContentNodeRendererContext.(N) -> Unit,
) {
    nodeRendererFactory { context ->
        typedNodeRenderer<N> { node -> block(context, node) }
    }
}
