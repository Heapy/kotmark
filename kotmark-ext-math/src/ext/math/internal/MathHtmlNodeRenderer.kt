package io.heapy.kotmark.ext.math.internal

import io.heapy.kotmark.ext.math.InlineMath
import io.heapy.kotmark.ext.math.MathBlock
import io.heapy.kotmark.renderer.html.HtmlRenderer
import io.heapy.kotmark.renderer.html.nodeRenderer

internal fun HtmlRenderer.Configuration.mathNodeRenderers() {
    nodeRenderer<InlineMath> { node ->
        writer.tag("span", extendAttributes(node, "span", mapOf("class" to "math math-inline")))
        writer.text(node.literal)
        writer.tag("/span")
    }
    nodeRenderer<MathBlock> { node ->
        writer.line()
        writer.tag("div", extendAttributes(node, "div", mapOf("class" to "math math-block")))
        writer.text(node.literal)
        writer.tag("/div")
        writer.line()
    }
}
