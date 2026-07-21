package org.commonmark.ext.gfm.alerts.internal

import org.commonmark.ext.gfm.alerts.Alert
import org.commonmark.ext.gfm.alerts.AlertTitle
import org.commonmark.node.Block
import org.commonmark.node.BlockQuote
import org.commonmark.node.Document
import org.commonmark.parser.InlineParser
import org.commonmark.parser.SourceLine
import org.commonmark.parser.SourceLines
import org.commonmark.parser.block.AbstractBlockParser
import org.commonmark.parser.block.AbstractBlockParserFactory
import org.commonmark.parser.block.BlockContinue
import org.commonmark.parser.block.BlockStart
import org.commonmark.parser.block.MatchedBlockParser
import org.commonmark.parser.block.ParserState
import org.commonmark.text.Characters

class AlertBlockParser private constructor(
    type: String,
    private val titleLine: SourceLine?,
) : AbstractBlockParser() {
    private val alert = Alert(type)

    override val block: Block
        get() = alert

    override fun isContainer(): Boolean = true

    override fun canContain(childBlock: Block): Boolean = true

    override fun tryContinue(parserState: ParserState): BlockContinue? {
        val line = parserState.line.content
        val nextNonSpace = parserState.nextNonSpaceIndex
        if (
            parserState.indent >= 4 ||
            nextNonSpace >= line.length ||
            line[nextNonSpace] != '>'
        ) {
            return BlockContinue.none()
        }

        var newColumn = parserState.column + parserState.indent + 1
        if (Characters.isSpaceOrTab(line, nextNonSpace + 1)) {
            newColumn++
        }
        return BlockContinue.atColumn(newColumn)
    }

    override fun parseInlines(inlineParser: InlineParser) {
        val line = titleLine ?: return
        if (line.content.isEmpty()) {
            return
        }
        val title = AlertTitle()
        inlineParser.parse(SourceLines.of(line), title)
        line.sourceSpan?.let { title.setSourceSpans(listOf(it)) }
        alert.prependChild(title)
    }

    class Factory(
        private val allowedTypes: Set<String>,
        private val customTitlesAllowed: Boolean,
        private val nestedAlertsAllowed: Boolean,
    ) : AbstractBlockParserFactory() {
        override fun tryStart(
            state: ParserState,
            matchedBlockParser: MatchedBlockParser,
        ): BlockStart? {
            if (state.indent >= 4) {
                return BlockStart.none()
            }

            val matched = matchedBlockParser.matchedBlockParser.block
            if (!nestedAlertsAllowed && !isAtRoot(matched)) {
                return BlockStart.none()
            }

            val line = state.line.content
            val nextNonSpace = state.nextNonSpaceIndex
            if (nextNonSpace < line.length && line[nextNonSpace] == '>') {
                return tryStartFresh(line, nextNonSpace, state)
            }
            if (matched is BlockQuote && matched.firstChild == null) {
                return tryStartFresh(line, nextNonSpace, state)
            }
            return BlockStart.none()
        }

        private fun tryStartFresh(
            line: CharSequence,
            nextNonSpace: Int,
            state: ParserState,
        ): BlockStart? {
            val afterGt =
                if (nextNonSpace < line.length && line[nextNonSpace] == '>') {
                    var index = nextNonSpace + 1
                    if (Characters.isSpaceOrTab(line, index)) {
                        index++
                    }
                    index
                } else {
                    state.index
                }

            val pattern =
                if (customTitlesAllowed) ALERT_PATTERN_CUSTOM_TITLE else ALERT_PATTERN_NO_CUSTOM_TITLE
            val match = pattern.matchEntire(line.subSequence(afterGt, line.length))
                ?: return BlockStart.none()
            val type = match.groupValues[1].uppercase()
            if (type !in allowedTypes) {
                return BlockStart.none()
            }

            val titleLine =
                if (customTitlesAllowed) {
                    var absoluteStart = afterGt + match.groupValues[1].length + 3
                    var absoluteEnd = afterGt + match.value.length
                    while (
                        absoluteStart < absoluteEnd &&
                        Characters.isSpaceOrTab(state.line.content, absoluteStart)
                    ) {
                        absoluteStart++
                    }
                    while (
                        absoluteEnd > absoluteStart &&
                        Characters.isSpaceOrTab(state.line.content, absoluteEnd - 1)
                    ) {
                        absoluteEnd--
                    }
                    state.line.substring(absoluteStart, absoluteEnd)
                } else {
                    null
                }

            var start: BlockStart =
                BlockStart.of(AlertBlockParser(type, titleLine)).atIndex(line.length)
            val active = state.activeBlockParser.block
            if (
                active is BlockQuote &&
                active.firstChild == null &&
                (nextNonSpace >= line.length || line[nextNonSpace] != '>')
            ) {
                @Suppress("DEPRECATION")
                start = start.replaceActiveBlockParser()
            }
            return start
        }

        private fun isAtRoot(matched: Block): Boolean =
            matched is Document ||
                (
                    matched is BlockQuote &&
                        matched.firstChild == null &&
                        matched.parent is Document
                )
    }

    companion object {
        private val ALERT_PATTERN_NO_CUSTOM_TITLE = Regex("^\\[!([a-zA-Z]+)]\\s*$")
        private val ALERT_PATTERN_CUSTOM_TITLE = Regex("^\\[!([a-zA-Z]+)](.*)$")
    }
}
