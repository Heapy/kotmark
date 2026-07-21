package org.commonmark.internal

import org.commonmark.node.DefinitionMap
import org.commonmark.node.LinkReferenceDefinition
import org.commonmark.node.Paragraph
import org.commonmark.node.SourceSpan
import org.commonmark.parser.InlineParser
import org.commonmark.parser.SourceLine
import org.commonmark.parser.SourceLines
import org.commonmark.parser.block.AbstractBlockParser
import org.commonmark.parser.block.BlockContinue
import org.commonmark.parser.block.ParserState

class ParagraphParser : AbstractBlockParser() {
    override val block: Paragraph = Paragraph()
    private val linkReferenceDefinitionParser = LinkReferenceDefinitionParser()

    override fun canHaveLazyContinuationLines(): Boolean = true

    override fun tryContinue(parserState: ParserState): BlockContinue? =
        if (!parserState.isBlank) {
            BlockContinue.atIndex(parserState.index)
        } else {
            BlockContinue.none()
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
