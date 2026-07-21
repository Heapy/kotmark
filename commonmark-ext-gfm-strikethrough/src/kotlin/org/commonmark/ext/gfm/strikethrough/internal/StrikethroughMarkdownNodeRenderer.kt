package org.commonmark.ext.gfm.strikethrough.internal

import org.commonmark.ext.gfm.strikethrough.Strikethrough
import org.commonmark.node.Node
import org.commonmark.renderer.markdown.MarkdownNodeRendererContext

class StrikethroughMarkdownNodeRenderer(
    private val context: MarkdownNodeRendererContext,
) : StrikethroughNodeRenderer() {
    override fun render(node: Node) {
        val strikethrough = node as Strikethrough
        context.writer.raw(strikethrough.openingDelimiter)
        renderChildren(node)
        context.writer.raw(strikethrough.closingDelimiter)
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
