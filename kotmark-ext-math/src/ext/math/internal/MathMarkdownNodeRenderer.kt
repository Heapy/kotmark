package io.heapy.kotmark.ext.math.internal

import io.heapy.kotmark.ext.math.InlineMath
import io.heapy.kotmark.ext.math.MathBlock
import io.heapy.kotmark.renderer.markdown.MarkdownRenderer
import io.heapy.kotmark.renderer.markdown.nodeRenderer

internal fun MarkdownRenderer.Configuration.mathNodeRenderers() {
    nodeRenderer<InlineMath>(specialCharacters = setOf('$')) { node ->
        writer.raw('$')
        writer.raw(node.literal)
        writer.raw('$')
    }
    nodeRenderer<MathBlock> { node ->
        val lines = if (node.literal.isEmpty()) emptyList() else node.literal.removeSuffix("\n").split('\n')
        // Indentation stripped by the parser can expose a fence-looking
        // content line. Restore three spaces so it cannot close the block.
        val indent = if (lines.any { line ->
            val spaces = line.takeWhile { it == ' ' }.length
            spaces in 1..3 && line.trim(' ', '\t') == "$$"
        }) "   " else ""
        writer.raw(indent + "$$")
        writer.line()
        // line() maintains list/blockquote prefixes; raw multiline text does not.
        for (line in lines) {
            writer.raw(indent + line)
            writer.line()
        }
        writer.raw(indent + "$$")
        writer.block()
    }
}
