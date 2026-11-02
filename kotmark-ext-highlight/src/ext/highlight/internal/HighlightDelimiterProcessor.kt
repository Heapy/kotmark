package io.heapy.kotmark.ext.highlight.internal

import io.heapy.kotmark.ext.highlight.Highlight
import io.heapy.kotmark.node.Nodes
import io.heapy.kotmark.node.SourceSpans
import io.heapy.kotmark.parser.delimiter.DelimiterProcessor
import io.heapy.kotmark.parser.delimiter.DelimiterRun

class HighlightDelimiterProcessor : DelimiterProcessor {
    override val openingCharacter: Char = '='
    override val closingCharacter: Char = '='
    override val minLength: Int = 2

    override fun process(
        openingRun: DelimiterRun,
        closingRun: DelimiterRun,
    ): Int {
        if (openingRun.originalLength() != 2 || closingRun.originalLength() != 2) {
            return 0
        }
        val opener = openingRun.opener
        val highlight = Highlight()
        val sourceSpans = SourceSpans.empty()
        sourceSpans.addAllFrom(openingRun.getOpeners(2))
        for (node in Nodes.between(opener, closingRun.closer)) {
            highlight.appendChild(node)
            sourceSpans.addAll(node.getSourceSpans())
        }
        sourceSpans.addAllFrom(closingRun.getClosers(2))
        highlight.setSourceSpans(sourceSpans.getSourceSpans())
        opener.insertAfter(highlight)
        return 2
    }
}
