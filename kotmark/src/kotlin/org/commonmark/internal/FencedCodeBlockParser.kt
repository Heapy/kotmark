package org.commonmark.internal

import org.commonmark.internal.util.Escaping
import org.commonmark.internal.util.Parsing
import org.commonmark.node.FencedCodeBlock
import org.commonmark.parser.SourceLine
import org.commonmark.parser.block.AbstractBlockParser
import org.commonmark.parser.block.AbstractBlockParserFactory
import org.commonmark.parser.block.BlockContinue
import org.commonmark.parser.block.BlockStart
import org.commonmark.parser.block.MatchedBlockParser
import org.commonmark.parser.block.ParserState
import org.commonmark.text.Characters

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
            return BlockContinue.finished()
        }

        var indentToSkip = block.fenceIndent
        while (indentToSkip > 0 && newIndex < line.length && line[newIndex] == ' ') {
            newIndex++
            indentToSkip--
        }
        return BlockContinue.atIndex(newIndex)
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
            var backticks = 0
            var tildes = 0
            for (currentIndex in index..<line.length) {
                when (line[currentIndex]) {
                    '`' -> backticks++
                    '~' -> tildes++
                    else -> break
                }
            }

            return when {
                backticks >= 3 && tildes == 0 -> {
                    if (Characters.find('`', line, index + backticks) != -1) {
                        null
                    } else {
                        FencedCodeBlockParser('`', backticks, indent)
                    }
                }

                tildes >= 3 && backticks == 0 ->
                    FencedCodeBlockParser('~', tildes, indent)

                else -> null
            }
        }
    }
}
