package io.heapy.kotmark.internal

import io.heapy.kotmark.node.DefinitionMap
import io.heapy.kotmark.node.LinkReferenceDefinition
import io.heapy.kotmark.node.Paragraph
import io.heapy.kotmark.node.SourceSpan
import io.heapy.kotmark.parser.InlineParser
import io.heapy.kotmark.parser.SourceLine
import io.heapy.kotmark.parser.SourceLines
import io.heapy.kotmark.parser.block.AbstractBlockParser
import io.heapy.kotmark.parser.block.BlockContinue
import io.heapy.kotmark.parser.block.ParserState

class ParagraphParser : AbstractBlockParser() {
    override val block: Paragraph = Paragraph()
    private val linkReferenceDefinitionParser = LinkReferenceDefinitionParser()

    override fun canHaveLazyContinuationLines(): Boolean = true

    override fun tryContinue(parserState: ParserState): BlockContinue? =
        if (!parserState.isBlank) {
            BlockContinue.AtIndex(parserState.index)
        } else {
            null
        }

    override fun addLine(line: SourceLine) {
        linkReferenceDefinitionParser.parse(line)
    }

    override fun addSourceSpan(sourceSpan: SourceSpan) {
        linkReferenceDefinitionParser.addSourceSpan(sourceSpan)
    }

    override val definitions: List<DefinitionMap<*>>
        get() {
            val map = DefinitionMap(LinkReferenceDefinition::class)
            for (definition in linkReferenceDefinitionParser.definitions) {
                map.putIfAbsent(requireNotNull(definition.label), definition)
            }
            return listOf(map)
        }

    override fun closeBlock() {
        for (definition in linkReferenceDefinitionParser.definitions) {
            block.insertBefore(definition)
        }

        if (linkReferenceDefinitionParser.paragraphLines.isEmpty()) {
            block.unlink()
        } else {
            block.setSourceSpans(linkReferenceDefinitionParser.paragraphSourceSpans)
        }
    }

    override fun parseInlines(inlineParser: InlineParser) {
        val lines = linkReferenceDefinitionParser.paragraphLines
        if (!lines.isEmpty()) {
            inlineParser.parse(lines, block)
        }
    }

    val paragraphLines: SourceLines
        get() = linkReferenceDefinitionParser.paragraphLines

    fun removeLines(lines: Int): List<SourceSpan> =
        linkReferenceDefinitionParser.removeLines(lines)
}
