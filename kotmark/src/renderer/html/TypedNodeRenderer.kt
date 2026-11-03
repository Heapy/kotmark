package io.heapy.kotmark.renderer.html

import io.heapy.kotmark.node.Node
import io.heapy.kotmark.renderer.typedNodeRenderer

/**
 * Registers a handler for exactly [N]; the first registration for a node type wins.
 * The receiver belongs to the current render. Captured mutable state in [block] is shared;
 * use [HtmlRenderer.Configuration.nodeRendererFactory] for state local to each render
 * or for lifecycle hooks. This helper does not change traversal or URL processing:
 * custom handlers remain responsible for their output.
 */
inline fun <reified N : Node> HtmlRenderer.Configuration.nodeRenderer(
    crossinline block: HtmlNodeRendererContext.(N) -> Unit,
) {
    nodeRendererFactory { context ->
        typedNodeRenderer<N> { node -> block(context, node) }
    }
}
