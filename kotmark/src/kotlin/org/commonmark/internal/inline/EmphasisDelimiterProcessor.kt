package org.commonmark.internal.inline

import org.commonmark.node.Emphasis
import org.commonmark.node.Node
import org.commonmark.node.Nodes
import org.commonmark.node.SourceSpans
import org.commonmark.node.StrongEmphasis
import org.commonmark.parser.delimiter.DelimiterProcessor
import org.commonmark.parser.delimiter.DelimiterRun

abstract class EmphasisDelimiterProcessor(
    private val delimiterChar: Char,
) : DelimiterProcessor {
    override val openingCharacter: Char
        get() = delimiterChar

    override val closingCharacter: Char
        get() = delimiterChar

    override val minLength: Int
        get() = 1

    override fun process(openingRun: DelimiterRun, closingRun: DelimiterRun): Int {
        if (
            (openingRun.canClose() || closingRun.canOpen()) &&
            closingRun.originalLength() % 3 != 0 &&
            (openingRun.originalLength() + closingRun.originalLength()) % 3 == 0
        ) {
            return 0
        }

        val usedDelimiters: Int
        val emphasis: Node
        if (openingRun.length() >= 2 && closingRun.length() >= 2) {
            usedDelimiters = 2
            emphasis = StrongEmphasis("$delimiterChar$delimiterChar")
        } else {
            usedDelimiters = 1
            emphasis = Emphasis(delimiterChar.toString())
        }

        val sourceSpans = SourceSpans.empty()
        sourceSpans.addAllFrom(openingRun.getOpeners(usedDelimiters))

        val opener = openingRun.opener
        for (node in Nodes.between(opener, closingRun.closer)) {
            emphasis.appendChild(node)
            sourceSpans.addAll(node.getSourceSpans())
        }

        sourceSpans.addAllFrom(closingRun.getClosers(usedDelimiters))
        emphasis.setSourceSpans(sourceSpans.getSourceSpans())
        opener.insertAfter(emphasis)
        return usedDelimiters
    }
}
