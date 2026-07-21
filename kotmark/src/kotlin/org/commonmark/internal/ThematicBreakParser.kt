package org.commonmark.internal

import org.commonmark.node.ThematicBreak
import org.commonmark.parser.block.AbstractBlockParser
import org.commonmark.parser.block.AbstractBlockParserFactory
import org.commonmark.parser.block.BlockContinue
import org.commonmark.parser.block.BlockStart
import org.commonmark.parser.block.MatchedBlockParser
import org.commonmark.parser.block.ParserState

class ThematicBreakParser(
    literal: String,
) : AbstractBlockParser() {
    override val block: ThematicBreak = ThematicBreak().apply {
        this.literal = literal
    }

    override fun tryContinue(parserState: ParserState): BlockContinue? =
        BlockContinue.none()

    class Factory : AbstractBlockParserFactory() {
        override fun tryStart(
            state: ParserState,
            matchedBlockParser: MatchedBlockParser,
        ): BlockStart? {
            if (state.indent >= 4) {
                return BlockStart.none()
            }

            val line = state.line.content
            return if (isThematicBreak(line, state.nextNonSpaceIndex)) {
                BlockStart.of(
                    ThematicBreakParser(line.subSequence(state.index, line.length).toString()),
                ).atIndex(line.length)
            } else {
                BlockStart.none()
            }
        }
    }

    companion object {
        internal fun isThematicBreak(line: CharSequence, index: Int): Boolean {
            var dashes = 0
            var underscores = 0
            var asterisks = 0
            for (currentIndex in index..<line.length) {
                when (line[currentIndex]) {
                    '-' -> dashes++
                    '_' -> underscores++
                    '*' -> asterisks++
                    ' ', '\t' -> Unit
                    else -> return false
                }
            }
            return (
                dashes >= 3 && underscores == 0 && asterisks == 0 ||
                    underscores >= 3 && dashes == 0 && asterisks == 0 ||
                    asterisks >= 3 && dashes == 0 && underscores == 0
            )
        }
    }
}
