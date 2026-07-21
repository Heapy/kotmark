package org.commonmark.internal

import org.commonmark.internal.util.Parsing
import org.commonmark.node.Block
import org.commonmark.node.IndentedCodeBlock
import org.commonmark.node.Paragraph
import org.commonmark.parser.SourceLine
import org.commonmark.parser.block.AbstractBlockParser
import org.commonmark.parser.block.AbstractBlockParserFactory
import org.commonmark.parser.block.BlockContinue
import org.commonmark.parser.block.BlockStart
import org.commonmark.parser.block.MatchedBlockParser
import org.commonmark.parser.block.ParserState
import org.commonmark.text.Characters

class IndentedCodeBlockParser : AbstractBlockParser() {
    override val block: IndentedCodeBlock = IndentedCodeBlock()
    private val lines = mutableListOf<CharSequence>()

    override fun tryContinue(parserState: ParserState): BlockContinue? =
        when {
            parserState.indent >= Parsing.CODE_BLOCK_INDENT ->
                BlockContinue.atColumn(parserState.column + Parsing.CODE_BLOCK_INDENT)

            parserState.isBlank ->
                BlockContinue.atIndex(parserState.nextNonSpaceIndex)

            else -> BlockContinue.none()
        }

    override fun addLine(line: SourceLine) {
        lines.add(line.content)
    }

    override fun closeBlock() {
        var lastNonBlank = lines.lastIndex
        while (lastNonBlank >= 0 && Characters.isBlank(lines[lastNonBlank])) {
            lastNonBlank--
        }

        block.literal = buildString {
            for (index in 0..lastNonBlank) {
                append(lines[index])
                append('\n')
            }
        }
    }

    class Factory : AbstractBlockParserFactory() {
        override fun tryStart(
            state: ParserState,
            matchedBlockParser: MatchedBlockParser,
        ): BlockStart? =
            if (
                state.indent >= Parsing.CODE_BLOCK_INDENT &&
                !state.isBlank &&
                state.activeBlockParser.block !is Paragraph
            ) {
                BlockStart.of(IndentedCodeBlockParser())
                    .atColumn(state.column + Parsing.CODE_BLOCK_INDENT)
            } else {
                BlockStart.none()
            }
    }
}
