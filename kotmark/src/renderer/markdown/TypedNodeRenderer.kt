package io.heapy.kotmark.renderer.markdown

import io.heapy.kotmark.node.Node
import io.heapy.kotmark.renderer.NodeRenderer
import io.heapy.kotmark.renderer.typedNodeRenderer

/**
 * Registers a handler for exactly [N]; the first registration for a node type wins.
 * [specialCharacters] is copied at registration and contributes to text escaping.
 * The receiver belongs to the current render. Captured mutable state in [block] is shared;
 * use [MarkdownRenderer.Configuration.nodeRendererFactory] for state local to each render.
 */
inline fun <reified N : Node> MarkdownRenderer.Configuration.nodeRenderer(
    specialCharacters: Set<Char> = emptySet(),
    crossinline block: MarkdownNodeRendererContext.(N) -> Unit,
) {
    val characters = specialCharacters.toSet()
    nodeRendererFactory(
        object : MarkdownNodeRendererFactory {
            override val specialCharacters: Set<Char> = characters

            override fun create(context: MarkdownNodeRendererContext): NodeRenderer =
                typedNodeRenderer<N> { node -> block(context, node) }
        },
    )
}
