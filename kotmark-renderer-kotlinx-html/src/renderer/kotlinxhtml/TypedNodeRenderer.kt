package io.heapy.kotmark.renderer.kotlinxhtml

import io.heapy.kotmark.node.Node
import io.heapy.kotmark.renderer.typedNodeRenderer

/**
 * Registers a handler for exactly [N]; the first registration for a node type wins.
 * The receiver belongs to the current render. Captured mutable state in [block] is shared;
 * use [KotlinxHtmlRenderer.Configuration.nodeRendererFactory] for state local to each render
 * or for lifecycle hooks. Custom handlers remain responsible for URL processing.
 */
inline fun <reified N : Node> KotlinxHtmlRenderer.Configuration.nodeRenderer(
    crossinline block: KotlinxHtmlNodeRendererContext.(N) -> Unit,
) {
    nodeRendererFactory { context ->
        typedNodeRenderer<N> { node -> block(context, node) }
    }
}
