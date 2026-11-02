package io.heapy.kotmark.ext.gfm.strikethrough.internal

import io.heapy.kotmark.node.Node
import io.heapy.kotmark.renderer.html.HtmlNodeRendererContext

class StrikethroughHtmlNodeRenderer(
    private val context: HtmlNodeRendererContext,
) : StrikethroughNodeRenderer() {
    override fun render(node: Node) {
        val html = context.writer
        html.tag("del", context.extendAttributes(node, "del", emptyMap()))
        renderChildren(node)
        html.tag("/del")
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
