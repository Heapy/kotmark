package org.commonmark.parser.block

import org.commonmark.node.Block
import org.commonmark.node.DefinitionMap
import org.commonmark.node.SourceSpan
import org.commonmark.parser.InlineParser
import org.commonmark.parser.SourceLine

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
