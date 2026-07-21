package org.commonmark.ext.gfm.strikethrough.internal

import org.commonmark.ext.gfm.strikethrough.Strikethrough
import org.commonmark.node.Nodes
import org.commonmark.node.SourceSpans
import org.commonmark.parser.delimiter.DelimiterProcessor
import org.commonmark.parser.delimiter.DelimiterRun

class StrikethroughDelimiterProcessor(
    private val requireTwoTildes: Boolean = false,
) : DelimiterProcessor {
    override val openingCharacter: Char = '~'
    override val closingCharacter: Char = '~'
    override val minLength: Int = if (requireTwoTildes) 2 else 1

    override fun process(
        openingRun: DelimiterRun,
        closingRun: DelimiterRun,
    ): Int {
        if (openingRun.length() != closingRun.length() || openingRun.length() > 2) {
            return 0
        }

        val opener = openingRun.opener
        val delimiter =
            if (openingRun.length() == 1) {
                opener.literal.orEmpty()
            } else {
                opener.literal.orEmpty().repeat(2)
            }
        val strikethrough = Strikethrough(delimiter)
        val sourceSpans = SourceSpans.empty()
        sourceSpans.addAllFrom(openingRun.getOpeners(openingRun.length()))
        for (node in Nodes.between(opener, closingRun.closer)) {
            strikethrough.appendChild(node)
            sourceSpans.addAll(node.getSourceSpans())
        }
        sourceSpans.addAllFrom(closingRun.getClosers(closingRun.length()))
        strikethrough.setSourceSpans(sourceSpans.getSourceSpans())
        opener.insertAfter(strikethrough)
        return openingRun.length()
    }
}
