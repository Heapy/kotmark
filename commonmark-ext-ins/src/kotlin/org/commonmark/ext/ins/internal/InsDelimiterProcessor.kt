package org.commonmark.ext.ins.internal

import org.commonmark.ext.ins.Ins
import org.commonmark.node.Nodes
import org.commonmark.node.SourceSpans
import org.commonmark.parser.delimiter.DelimiterProcessor
import org.commonmark.parser.delimiter.DelimiterRun

class InsDelimiterProcessor : DelimiterProcessor {
    override val openingCharacter: Char = '+'
    override val closingCharacter: Char = '+'
    override val minLength: Int = 2

    override fun process(
        openingRun: DelimiterRun,
        closingRun: DelimiterRun,
    ): Int {
        if (openingRun.length() < 2 || closingRun.length() < 2) {
            return 0
        }
        val opener = openingRun.opener
        val ins = Ins()
        val sourceSpans = SourceSpans.empty()
        sourceSpans.addAllFrom(openingRun.getOpeners(2))
        for (node in Nodes.between(opener, closingRun.closer)) {
            ins.appendChild(node)
            sourceSpans.addAll(node.getSourceSpans())
        }
        sourceSpans.addAllFrom(closingRun.getClosers(2))
        ins.setSourceSpans(sourceSpans.getSourceSpans())
        opener.insertAfter(ins)
        return 2
    }
}
