package io.heapy.kotmark.ext.ins.internal

import io.heapy.kotmark.ext.ins.Ins
import io.heapy.kotmark.node.Nodes
import io.heapy.kotmark.node.SourceSpans
import io.heapy.kotmark.parser.delimiter.DelimiterProcessor
import io.heapy.kotmark.parser.delimiter.DelimiterRun

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
