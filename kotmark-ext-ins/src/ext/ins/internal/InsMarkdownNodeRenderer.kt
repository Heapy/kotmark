package io.heapy.kotmark.ext.ins.internal

import io.heapy.kotmark.node.Node
import io.heapy.kotmark.renderer.markdown.MarkdownNodeRendererContext

class InsMarkdownNodeRenderer(
    private val context: MarkdownNodeRendererContext,
) : InsNodeRenderer() {
    override fun render(node: Node) {
        context.writer.raw("++")
        renderChildren(node)
        context.writer.raw("++")
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
