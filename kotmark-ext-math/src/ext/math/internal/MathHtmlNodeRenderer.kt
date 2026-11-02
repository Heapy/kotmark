package io.heapy.kotmark.ext.math.internal

import io.heapy.kotmark.ext.math.InlineMath
import io.heapy.kotmark.ext.math.MathBlock
import io.heapy.kotmark.node.Node
import io.heapy.kotmark.renderer.html.HtmlNodeRendererContext

internal class MathHtmlNodeRenderer(private val context: HtmlNodeRendererContext) : MathNodeRenderer() {
    override fun render(node: Node) {
        val block = node is MathBlock
        val tag = if (block) "div" else "span"
        val literal = when (node) {
            is InlineMath -> node.literal
            is MathBlock -> node.literal
            else -> return
        }
        val writer = context.writer
        if (block) writer.line()
        writer.tag(tag, context.extendAttributes(node, tag, mapOf("class" to if (block) "math math-block" else "math math-inline")))
        writer.text(literal)
        writer.tag("/$tag")
        if (block) writer.line()
    }
}
