package io.heapy.kotmark.ext.math.internal

import io.heapy.kotmark.ext.math.InlineMath
import io.heapy.kotmark.ext.math.MathBlock
import io.heapy.kotmark.renderer.text.LineBreakRendering
import io.heapy.kotmark.renderer.text.TextContentRenderer
import io.heapy.kotmark.renderer.text.nodeRenderer

internal fun TextContentRenderer.Configuration.mathNodeRenderers() {
    nodeRenderer<InlineMath> { node -> writer.write(node.literal) }
    nodeRenderer<MathBlock> { node ->
        val literal = node.literal.removeSuffix("\n")
        if (lineBreakRendering == LineBreakRendering.STRIP) {
            writer.writeStripped(literal)
        } else {
            writer.write(literal)
        }
        writer.block()
    }
}
