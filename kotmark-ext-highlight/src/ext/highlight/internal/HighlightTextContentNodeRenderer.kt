package io.heapy.kotmark.ext.highlight.internal

import io.heapy.kotmark.node.Node
import io.heapy.kotmark.renderer.text.TextContentNodeRendererContext

class HighlightTextContentNodeRenderer(
    private val context: TextContentNodeRendererContext,
) : HighlightNodeRenderer() {
    override fun render(node: Node) {
        var child = node.firstChild
        while (child != null) {
            val next = child.next
            context.render(child)
            child = next
        }
    }
}
