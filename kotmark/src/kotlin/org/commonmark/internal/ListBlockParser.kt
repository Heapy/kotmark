package org.commonmark.internal

import org.commonmark.internal.util.Parsing
import org.commonmark.node.Block
import org.commonmark.node.BulletList
import org.commonmark.node.ListBlock
import org.commonmark.node.ListItem
import org.commonmark.node.OrderedList
import org.commonmark.parser.block.AbstractBlockParser
import org.commonmark.parser.block.AbstractBlockParserFactory
import org.commonmark.parser.block.BlockContinue
import org.commonmark.parser.block.BlockStart
import org.commonmark.parser.block.MatchedBlockParser
import org.commonmark.parser.block.ParserState

class ListBlockParser(
    override val block: ListBlock,
) : AbstractBlockParser() {
    private var hadBlankLine: Boolean = false
    private var linesAfterBlank: Int = 0

    override fun isContainer(): Boolean = true

    override fun canContain(childBlock: Block): Boolean {
        if (childBlock !is ListItem) {
            return false
        }
        if (hadBlankLine && linesAfterBlank == 1) {
            block.isTight = false
            hadBlankLine = false
        }
        return true
    }

    override fun tryContinue(parserState: ParserState): BlockContinue {
        if (parserState.isBlank) {
            hadBlankLine = true
            linesAfterBlank = 0
        } else if (hadBlankLine) {
            linesAfterBlank++
        }
        return BlockContinue.atIndex(parserState.index)
    }

    class Factory : AbstractBlockParserFactory() {
        override fun tryStart(
            state: ParserState,
            matchedBlockParser: MatchedBlockParser,
        ): BlockStart? {
            val matched = matchedBlockParser.matchedBlockParser
            if (state.indent >= Parsing.CODE_BLOCK_INDENT) {
                return BlockStart.none()
            }

            val markerIndex = state.nextNonSpaceIndex
            val markerColumn = state.column + state.indent
            val inParagraph = !matchedBlockParser.paragraphLines.isEmpty()
            val listData = parseList(state.line.content, markerIndex, markerColumn, inParagraph)
                ?: return BlockStart.none()

            val newColumn = listData.contentColumn
            val listItemParser = ListItemParser(state.indent, newColumn - state.column)
            if (
                matched !is ListBlockParser ||
                !listsMatch(matched.block, listData.listBlock)
            ) {
                listData.listBlock.isTight = true
                return BlockStart.of(ListBlockParser(listData.listBlock), listItemParser)
                    .atColumn(newColumn)
            }
            return BlockStart.of(listItemParser).atColumn(newColumn)
        }
    }

    private data class ListData(
        val listBlock: ListBlock,
        val contentColumn: Int,
    )

    private data class ListMarkerData(
        val listBlock: ListBlock,
        val indexAfterMarker: Int,
    )

    companion object {
        private fun parseList(
            line: CharSequence,
            markerIndex: Int,
            markerColumn: Int,
            inParagraph: Boolean,
        ): ListData? {
            val listMarker = parseListMarker(line, markerIndex) ?: return null
            val listBlock = listMarker.listBlock
            val indexAfterMarker = listMarker.indexAfterMarker
            val markerLength = indexAfterMarker - markerIndex
            val columnAfterMarker = markerColumn + markerLength
            var contentColumn = columnAfterMarker

            var hasContent = false
            for (index in indexAfterMarker..<line.length) {
                when (line[index]) {
                    '\t' -> contentColumn += Parsing.columnsToNextTabStop(contentColumn)
                    ' ' -> contentColumn++
                    else -> {
                        hasContent = true
                        break
                    }
                }
            }

            if (inParagraph) {
                if (listBlock is OrderedList && listBlock.markerStartNumber != 1) {
                    return null
                }
                if (!hasContent) {
                    return null
                }
            }

            if (!hasContent || contentColumn - columnAfterMarker > Parsing.CODE_BLOCK_INDENT) {
                contentColumn = columnAfterMarker + 1
            }
            return ListData(listBlock, contentColumn)
        }

        private fun parseListMarker(line: CharSequence, index: Int): ListMarkerData? =
            when (val marker = line[index]) {
                '-', '+', '*' ->
                    if (isSpaceTabOrEnd(line, index + 1)) {
                        ListMarkerData(
                            BulletList().apply { this.marker = marker.toString() },
                            index + 1,
                        )
                    } else {
                        null
                    }

                else -> parseOrderedList(line, index)
            }

        private fun parseOrderedList(line: CharSequence, index: Int): ListMarkerData? {
            var digits = 0
            for (currentIndex in index..<line.length) {
                when (val current = line[currentIndex]) {
                    in '0'..'9' -> {
                        digits++
                        if (digits > 9) return null
                    }

                    '.', ')' -> {
                        if (digits < 1 || !isSpaceTabOrEnd(line, currentIndex + 1)) {
                            return null
                        }
                        return ListMarkerData(
                            OrderedList().apply {
                                markerStartNumber =
                                    line.subSequence(index, currentIndex).toString().toInt()
                                markerDelimiter = current.toString()
                            },
                            currentIndex + 1,
                        )
                    }

                    else -> return null
                }
            }
            return null
        }

        private fun isSpaceTabOrEnd(line: CharSequence, index: Int): Boolean =
            index >= line.length || line[index] == ' ' || line[index] == '\t'

        private fun listsMatch(first: ListBlock, second: ListBlock): Boolean =
            when {
                first is BulletList && second is BulletList ->
                    first.marker == second.marker

                first is OrderedList && second is OrderedList ->
                    first.markerDelimiter == second.markerDelimiter

                else -> false
            }
    }
}
