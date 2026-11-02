package io.heapy.kotmark.ext.gfm.strikethrough.internal

import io.heapy.kotmark.node.Node
import io.heapy.kotmark.renderer.text.TextContentNodeRendererContext

class StrikethroughTextContentNodeRenderer(
    private val context: TextContentNodeRendererContext,
) : StrikethroughNodeRenderer() {
    override fun render(node: Node) {
        context.writer.write('/')
        renderChildren(node)
        context.writer.write('/')
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
