package io.heapy.kotmark.ext.highlight.internal

import io.heapy.kotmark.node.Node
import io.heapy.kotmark.renderer.html.HtmlNodeRendererContext

class HighlightHtmlNodeRenderer(
    private val context: HtmlNodeRendererContext,
) : HighlightNodeRenderer() {
    override fun render(node: Node) {
        context.writer.tag("mark", context.extendAttributes(node, "mark", emptyMap()))
        renderChildren(node)
        context.writer.tag("/mark")
    }

    private fun renderChildren(parent: Node) {
        var node = parent.firstChild
        while (node != null) {
            val next = node.next
            context.render(node)
            node = next
        }
    }
}
