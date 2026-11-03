package io.heapy.kotmark.internal

import io.heapy.kotmark.internal.util.Escaping
import io.heapy.kotmark.internal.util.Parsing
import io.heapy.kotmark.node.FencedCodeBlock
import io.heapy.kotmark.parser.SourceLine
import io.heapy.kotmark.parser.block.AbstractBlockParser
import io.heapy.kotmark.parser.block.AbstractBlockParserFactory
import io.heapy.kotmark.parser.block.BlockContinue
import io.heapy.kotmark.parser.block.BlockStart
import io.heapy.kotmark.parser.block.MatchedBlockParser
import io.heapy.kotmark.parser.block.ParserState
import io.heapy.kotmark.text.Characters

class FencedCodeBlockParser(
    private val fenceChar: Char,
    private val openingFenceLength: Int,
    fenceIndent: Int,
) : AbstractBlockParser() {
    override val block: FencedCodeBlock = FencedCodeBlock().apply {
        fenceCharacter = this@FencedCodeBlockParser.fenceChar.toString()
        openingFenceLength = this@FencedCodeBlockParser.openingFenceLength
        this.fenceIndent = fenceIndent
    }

    private var firstLine: String? = null
    private val otherLines = StringBuilder()

    override fun tryContinue(parserState: ParserState): BlockContinue {
        val nextNonSpace = parserState.nextNonSpaceIndex
        var newIndex = parserState.index
        val line = parserState.line.content
        if (
            parserState.indent < Parsing.CODE_BLOCK_INDENT &&
            nextNonSpace < line.length &&
            tryClosing(line, nextNonSpace)
        ) {
            return BlockContinue.Finished
        }

        var indentToSkip = block.fenceIndent
        while (indentToSkip > 0 && newIndex < line.length && line[newIndex] == ' ') {
            newIndex++
            indentToSkip--
        }
        return BlockContinue.AtIndex(newIndex)
    }

    override fun addLine(line: SourceLine) {
        if (firstLine == null) {
            firstLine = line.content.toString()
        } else {
            otherLines.append(line.content)
            otherLines.append('\n')
        }
    }

    override fun closeBlock() {
        block.info = Escaping.unescapeString(firstLine.orEmpty().trim())
        block.literal = otherLines.toString()
    }

    private fun tryClosing(line: CharSequence, index: Int): Boolean {
        val fences = Characters.skip(fenceChar, line, index, line.length) - index
        if (fences < openingFenceLength) {
            return false
        }

        val after = Characters.skipSpaceTab(line, index + fences, line.length)
        if (after == line.length) {
            block.closingFenceLength = fences
            return true
        }
        return false
    }

    class Factory : AbstractBlockParserFactory() {
        override fun tryStart(
            state: ParserState,
            matchedBlockParser: MatchedBlockParser,
        ): BlockStart? {
            val indent = state.indent
            if (indent >= Parsing.CODE_BLOCK_INDENT) {
                return BlockStart.none()
            }

            val nextNonSpace = state.nextNonSpaceIndex
            val blockParser = checkOpener(state.line.content, nextNonSpace, indent)
                ?: return BlockStart.none()
            return BlockStart.of(blockParser)
                .atIndex(nextNonSpace + requireNotNull(blockParser.block.openingFenceLength))
        }
    }

    companion object {
        private fun checkOpener(
            line: CharSequence,
            index: Int,
            indent: Int,
        ): FencedCodeBlockParser? {
            // Only the leading run is the fence; the other character can start the info string.
            val length = line.length
            val backticks = Characters.skip('`', line, index, length) - index
            val tildes = Characters.skip('~', line, index, length) - index

            return when {
                backticks >= 3 -> {
                    if (Characters.find('`', line, index + backticks) != -1) {
                        null
                    } else {
                        FencedCodeBlockParser('`', backticks, indent)
                    }
                }

                tildes >= 3 ->
                    FencedCodeBlockParser('~', tildes, indent)

                else -> null
            }
        }
    }
}
