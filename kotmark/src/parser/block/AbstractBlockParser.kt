package io.heapy.kotmark.parser.block

import io.heapy.kotmark.node.Block
import io.heapy.kotmark.node.DefinitionMap
import io.heapy.kotmark.node.SourceSpan
import io.heapy.kotmark.parser.InlineParser
import io.heapy.kotmark.parser.SourceLine

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
