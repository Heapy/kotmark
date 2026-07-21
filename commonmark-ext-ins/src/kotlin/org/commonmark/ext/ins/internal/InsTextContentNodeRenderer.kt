package org.commonmark.ext.ins.internal

import org.commonmark.node.Node
import org.commonmark.renderer.text.TextContentNodeRendererContext

class InsTextContentNodeRenderer(
    private val context: TextContentNodeRendererContext,
) : InsNodeRenderer() {
    override fun render(node: Node) {
        var child = node.firstChild
        while (child != null) {
            val next = child.next
            context.render(child)
            child = next
        }
    }
}
