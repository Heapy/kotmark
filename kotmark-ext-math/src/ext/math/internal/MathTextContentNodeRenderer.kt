package io.heapy.kotmark.ext.math.internal

import io.heapy.kotmark.ext.math.InlineMath
import io.heapy.kotmark.ext.math.MathBlock
import io.heapy.kotmark.node.Node
import io.heapy.kotmark.renderer.text.LineBreakRendering
import io.heapy.kotmark.renderer.text.TextContentNodeRendererContext

internal class MathTextContentNodeRenderer(private val context: TextContentNodeRendererContext) : MathNodeRenderer() {
    override fun render(node: Node) {
        when (node) {
            is InlineMath -> context.writer.write(node.literal)
            is MathBlock -> {
                val literal = node.literal.removeSuffix("\n")
                if (context.lineBreakRendering == LineBreakRendering.STRIP) {
                    context.writer.writeStripped(literal)
                } else {
                    context.writer.write(literal)
                }
                context.writer.block()
            }
        }
    }
}
