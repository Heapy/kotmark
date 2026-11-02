package io.heapy.kotmark.internal

import io.heapy.kotmark.internal.util.Parsing
import io.heapy.kotmark.node.Block
import io.heapy.kotmark.node.IndentedCodeBlock
import io.heapy.kotmark.node.Paragraph
import io.heapy.kotmark.parser.SourceLine
import io.heapy.kotmark.parser.block.AbstractBlockParser
import io.heapy.kotmark.parser.block.AbstractBlockParserFactory
import io.heapy.kotmark.parser.block.BlockContinue
import io.heapy.kotmark.parser.block.BlockStart
import io.heapy.kotmark.parser.block.MatchedBlockParser
import io.heapy.kotmark.parser.block.ParserState
import io.heapy.kotmark.text.Characters

class IndentedCodeBlockParser : AbstractBlockParser() {
    override val block: IndentedCodeBlock = IndentedCodeBlock()
    private val lines = mutableListOf<CharSequence>()

    override fun tryContinue(parserState: ParserState): BlockContinue? =
        when {
            parserState.indent >= Parsing.CODE_BLOCK_INDENT ->
                BlockContinue.AtColumn(parserState.column + Parsing.CODE_BLOCK_INDENT)

            parserState.isBlank ->
                BlockContinue.AtIndex(parserState.nextNonSpaceIndex)

            else -> null
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
