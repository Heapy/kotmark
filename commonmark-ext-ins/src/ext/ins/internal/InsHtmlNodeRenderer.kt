package io.heapy.kotmark.ext.ins.internal

import io.heapy.kotmark.node.Node
import io.heapy.kotmark.renderer.html.HtmlNodeRendererContext

class InsHtmlNodeRenderer(
    private val context: HtmlNodeRendererContext,
) : InsNodeRenderer() {
    override fun render(node: Node) {
        context.writer.tag("ins", context.extendAttributes(node, "ins", emptyMap()))
        renderChildren(node)
        context.writer.tag("/ins")
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
