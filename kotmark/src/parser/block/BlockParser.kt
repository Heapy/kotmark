package io.heapy.kotmark.parser.block

import io.heapy.kotmark.node.Block
import io.heapy.kotmark.node.DefinitionMap
import io.heapy.kotmark.node.SourceSpan
import io.heapy.kotmark.parser.InlineParser
import io.heapy.kotmark.parser.SourceLine

interface BlockParser {
    fun isContainer(): Boolean

    fun canHaveLazyContinuationLines(): Boolean

    fun canContain(childBlock: Block): Boolean

    val block: Block

    fun tryContinue(parserState: ParserState): BlockContinue?

    fun addLine(line: SourceLine)

    fun addSourceSpan(sourceSpan: SourceSpan)

    val definitions: List<DefinitionMap<*>>

    fun closeBlock()

    fun parseInlines(inlineParser: InlineParser)
}
