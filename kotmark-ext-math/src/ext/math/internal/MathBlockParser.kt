package io.heapy.kotmark.ext.math.internal

import io.heapy.kotmark.ext.math.MathBlock
import io.heapy.kotmark.parser.SourceLine
import io.heapy.kotmark.parser.block.AbstractBlockParser
import io.heapy.kotmark.parser.block.AbstractBlockParserFactory
import io.heapy.kotmark.parser.block.BlockContinue
import io.heapy.kotmark.parser.block.BlockStart
import io.heapy.kotmark.parser.block.MatchedBlockParser
import io.heapy.kotmark.parser.block.ParserState

internal class MathBlockParser(private val fenceIndent: Int) : AbstractBlockParser() {
    override val block = MathBlock()
    private var openingLine = true
    private val content = StringBuilder()

    override fun tryContinue(parserState: ParserState): BlockContinue {
        if (isFence(parserState)) return BlockContinue.Finished
        var index = parserState.index
        var remaining = fenceIndent
        val line = parserState.line.content
        while (remaining > 0 && index < line.length && line[index] == ' ') {
            index++
            remaining--
        }
        return BlockContinue.AtIndex(index)
    }

    override fun addLine(line: SourceLine) {
        if (openingLine) {
            openingLine = false
        } else {
            content.append(line.content).append('\n')
        }
    }

    override fun closeBlock() {
        block.literal = content.toString()
    }

    class Factory : AbstractBlockParserFactory() {
        override fun tryStart(state: ParserState, matchedBlockParser: MatchedBlockParser): BlockStart? {
            if (!isFence(state)) return null
            return BlockStart.of(MathBlockParser(state.indent)).atIndex(state.line.content.length)
        }
    }

    companion object {
        private fun isFence(state: ParserState): Boolean =
            state.indent < 4 &&
                state.line.content.subSequence(state.nextNonSpaceIndex, state.line.content.length)
                    .trimEnd(' ', '\t') == "$$"
    }
}
