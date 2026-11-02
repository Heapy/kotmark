package io.heapy.kotmark.internal

import io.heapy.kotmark.internal.util.Parsing
import io.heapy.kotmark.node.Heading
import io.heapy.kotmark.parser.InlineParser
import io.heapy.kotmark.parser.SourceLine
import io.heapy.kotmark.parser.SourceLines
import io.heapy.kotmark.parser.beta.Scanner
import io.heapy.kotmark.parser.block.AbstractBlockParser
import io.heapy.kotmark.parser.block.AbstractBlockParserFactory
import io.heapy.kotmark.parser.block.BlockContinue
import io.heapy.kotmark.parser.block.BlockStart
import io.heapy.kotmark.parser.block.MatchedBlockParser
import io.heapy.kotmark.parser.block.ParserState
import io.heapy.kotmark.text.Characters

class HeadingParser(
    level: Int,
    private val content: SourceLines,
) : AbstractBlockParser() {
    override val block: Heading = Heading().apply {
        this.level = level
    }

    override fun tryContinue(parserState: ParserState): BlockContinue? =
        null

    override fun parseInlines(inlineParser: InlineParser) {
        inlineParser.parse(content, block)
    }

    class Factory : AbstractBlockParserFactory() {
        override fun tryStart(
            state: ParserState,
            matchedBlockParser: MatchedBlockParser,
        ): BlockStart? {
            if (state.indent >= Parsing.CODE_BLOCK_INDENT) {
                return BlockStart.none()
            }

            val line = state.line
            val nextNonSpace = state.nextNonSpaceIndex
            if (line.content[nextNonSpace] == '#') {
                val atxHeading = getAtxHeading(line.substring(nextNonSpace, line.content.length))
                if (atxHeading != null) {
                    return BlockStart.of(atxHeading).atIndex(line.content.length)
                }
            }

            val setextHeadingLevel = getSetextHeadingLevel(line.content, nextNonSpace)
            if (setextHeadingLevel > 0) {
                val paragraph = matchedBlockParser.paragraphLines
                if (!paragraph.isEmpty()) {
                    return BlockStart.of(HeadingParser(setextHeadingLevel, paragraph))
                        .atIndex(line.content.length)
                        .replaceParagraphLines(paragraph.lines.size)
                }
            }
            return BlockStart.none()
        }
    }

    companion object {
        private fun getAtxHeading(line: SourceLine): HeadingParser? {
            val scanner = Scanner.of(SourceLines.of(line))
            val level = scanner.matchMultiple('#')
            if (level == 0 || level > 6) {
                return null
            }
            if (!scanner.hasNext()) {
                return HeadingParser(level, SourceLines.empty())
            }
            if (scanner.peek() != ' ' && scanner.peek() != '\t') {
                return null
            }

            scanner.whitespace()
            val start = scanner.position()
            var end = start
            var hashCanEnd = true
            while (scanner.hasNext()) {
                when (scanner.peek()) {
                    '#' -> {
                        if (hashCanEnd) {
                            scanner.matchMultiple('#')
                            val whitespace = scanner.whitespace()
                            if (scanner.hasNext()) {
                                end = scanner.position()
                            }
                            hashCanEnd = whitespace > 0
                        } else {
                            scanner.next()
                            end = scanner.position()
                        }
                    }

                    ' ', '\t' -> {
                        hashCanEnd = true
                        scanner.next()
                    }

                    else -> {
                        hashCanEnd = false
                        scanner.next()
                        end = scanner.position()
                    }
                }
            }

            val source = scanner.getSource(start, end)
            return if (source.content.isEmpty()) {
                HeadingParser(level, SourceLines.empty())
            } else {
                HeadingParser(level, source)
            }
        }

        private fun getSetextHeadingLevel(line: CharSequence, index: Int): Int =
            when (line[index]) {
                '=' -> if (isSetextHeadingRest(line, index + 1, '=')) 1 else 0
                '-' -> if (isSetextHeadingRest(line, index + 1, '-')) 2 else 0
                else -> 0
            }

        private fun isSetextHeadingRest(
            line: CharSequence,
            index: Int,
            marker: Char,
        ): Boolean {
            val afterMarker = Characters.skip(marker, line, index, line.length)
            val afterSpace = Characters.skipSpaceTab(line, afterMarker, line.length)
            return afterSpace >= line.length
        }
    }
}
