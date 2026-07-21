package org.commonmark.ext.ins.internal

import org.commonmark.node.Node
import org.commonmark.renderer.html.HtmlNodeRendererContext

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
