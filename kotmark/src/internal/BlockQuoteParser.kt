package io.heapy.kotmark.internal

import io.heapy.kotmark.internal.util.Parsing
import io.heapy.kotmark.node.Block
import io.heapy.kotmark.node.BlockQuote
import io.heapy.kotmark.parser.block.AbstractBlockParser
import io.heapy.kotmark.parser.block.AbstractBlockParserFactory
import io.heapy.kotmark.parser.block.BlockContinue
import io.heapy.kotmark.parser.block.BlockStart
import io.heapy.kotmark.parser.block.MatchedBlockParser
import io.heapy.kotmark.parser.block.ParserState
import io.heapy.kotmark.text.Characters

class BlockQuoteParser : AbstractBlockParser() {
    override val block: BlockQuote = BlockQuote()

    override fun isContainer(): Boolean = true

    override fun canContain(childBlock: Block): Boolean = true

    override fun tryContinue(parserState: ParserState): BlockContinue? {
        val nextNonSpace = parserState.nextNonSpaceIndex
        if (!isMarker(parserState, nextNonSpace)) {
            return null
        }

        var newColumn = parserState.column + parserState.indent + 1
        if (Characters.isSpaceOrTab(parserState.line.content, nextNonSpace + 1)) {
            newColumn++
        }
        return BlockContinue.AtColumn(newColumn)
    }

    class Factory : AbstractBlockParserFactory() {
        override fun tryStart(
            state: ParserState,
            matchedBlockParser: MatchedBlockParser,
        ): BlockStart? {
            val nextNonSpace = state.nextNonSpaceIndex
            if (!isMarker(state, nextNonSpace)) {
                return BlockStart.none()
            }

            var newColumn = state.column + state.indent + 1
            if (Characters.isSpaceOrTab(state.line.content, nextNonSpace + 1)) {
                newColumn++
            }
            return BlockStart.of(BlockQuoteParser()).atColumn(newColumn)
        }
    }

    companion object {
        private fun isMarker(state: ParserState, index: Int): Boolean {
            val line = state.line.content
            return state.indent < Parsing.CODE_BLOCK_INDENT &&
                index < line.length &&
                line[index] == '>'
        }
    }
}
