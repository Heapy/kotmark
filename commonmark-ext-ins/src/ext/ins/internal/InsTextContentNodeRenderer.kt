package io.heapy.kotmark.ext.ins.internal

import io.heapy.kotmark.node.Node
import io.heapy.kotmark.renderer.text.TextContentNodeRendererContext

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
