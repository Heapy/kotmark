package org.commonmark.parser.block

import org.commonmark.node.Block
import org.commonmark.node.DefinitionMap
import org.commonmark.node.SourceSpan
import org.commonmark.parser.InlineParser
import org.commonmark.parser.SourceLine

abstract class AbstractBlockParser : BlockParser {
    override fun isContainer(): Boolean = false

    override fun canHaveLazyContinuationLines(): Boolean = false

    override fun canContain(childBlock: Block): Boolean = false

    override fun addLine(line: SourceLine) = Unit

    override fun addSourceSpan(sourceSpan: SourceSpan) {
        block.addSourceSpan(sourceSpan)
    }

    override val definitions: List<DefinitionMap<*>>
        get() = emptyList()

    override fun closeBlock() = Unit

    override fun parseInlines(inlineParser: InlineParser) = Unit
}
