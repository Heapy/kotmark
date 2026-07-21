package org.commonmark.internal

import org.commonmark.internal.util.Parsing
import org.commonmark.node.Heading
import org.commonmark.parser.InlineParser
import org.commonmark.parser.SourceLine
import org.commonmark.parser.SourceLines
import org.commonmark.parser.beta.Scanner
import org.commonmark.parser.block.AbstractBlockParser
import org.commonmark.parser.block.AbstractBlockParserFactory
import org.commonmark.parser.block.BlockContinue
import org.commonmark.parser.block.BlockStart
import org.commonmark.parser.block.MatchedBlockParser
import org.commonmark.parser.block.ParserState
import org.commonmark.text.Characters

class HeadingParser(
    level: Int,
    private val content: SourceLines,
) : AbstractBlockParser() {
    override val block: Heading = Heading().apply {
        this.level = level
    }

    override fun tryContinue(parserState: ParserState): BlockContinue? =
        BlockContinue.none()

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
