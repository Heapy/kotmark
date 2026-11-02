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

    /**
     * Continue at an absolute index or column, finish the block and consume this line,
     * or return `null` when this block does not continue. A `null` result leaves the line
     * available to the enclosing parser, including its lazy-continuation handling.
     */
    fun tryContinue(parserState: ParserState): BlockContinue?

    fun addLine(line: SourceLine)

    fun addSourceSpan(sourceSpan: SourceSpan)

    val definitions: List<DefinitionMap<*>>

    fun closeBlock()

    fun parseInlines(inlineParser: InlineParser)
}
