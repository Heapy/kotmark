package io.heapy.kotmark.ext.gfm.tables.internal

import io.heapy.kotmark.ext.gfm.tables.TableBlock
import io.heapy.kotmark.ext.gfm.tables.TableBody
import io.heapy.kotmark.ext.gfm.tables.TableCell
import io.heapy.kotmark.ext.gfm.tables.TableHead
import io.heapy.kotmark.ext.gfm.tables.TableRow
import io.heapy.kotmark.node.Block
import io.heapy.kotmark.parser.InlineParser
import io.heapy.kotmark.parser.SourceLine
import io.heapy.kotmark.parser.SourceLines
import io.heapy.kotmark.parser.block.AbstractBlockParser
import io.heapy.kotmark.parser.block.AbstractBlockParserFactory
import io.heapy.kotmark.parser.block.BlockContinue
import io.heapy.kotmark.parser.block.BlockStart
import io.heapy.kotmark.parser.block.MatchedBlockParser
import io.heapy.kotmark.parser.block.ParserState
import io.heapy.kotmark.text.Characters

class TableBlockParser private constructor(
    private val columns: List<TableCellInfo>,
    headerLine: SourceLine,
) : AbstractBlockParser() {
    private val table = TableBlock()
    private val rowLines = mutableListOf(headerLine)
    private var lazyContinuationLines = true

    override fun canHaveLazyContinuationLines(): Boolean = lazyContinuationLines

    override val block: Block
        get() = table

    override fun tryContinue(parserState: ParserState): BlockContinue? {
        val content = parserState.line.content
        val pipe = Characters.find('|', content, parserState.nextNonSpaceIndex)
        if (pipe == -1) {
            return null
        }
        if (
            pipe == parserState.nextNonSpaceIndex &&
            Characters.skipSpaceTab(content, pipe + 1, content.length) == content.length
        ) {
            lazyContinuationLines = false
            return null
        }
        return BlockContinue.AtIndex(parserState.index)
    }

    override fun addLine(line: SourceLine) {
        rowLines.add(line)
    }

    override fun parseInlines(inlineParser: InlineParser) {
        val sourceSpans = table.getSourceSpans()
        val headerSourceSpan = sourceSpans.firstOrNull()
        val head = TableHead()
        headerSourceSpan?.let(head::addSourceSpan)
        table.appendChild(head)

        val headerRow = TableRow()
        headerRow.setSourceSpans(head.getSourceSpans())
        head.appendChild(headerRow)
        val headerCells = split(rowLines[0])
        val headerColumnCount = headerCells.size
        for ([index, cell] in headerCells.withIndex()) {
            headerRow.appendChild(
                parseCell(cell, index, inlineParser).apply { isHeader = true },
            )
        }

        var body: TableBody? = null
        for (rowIndex in 2 until rowLines.size) {
            val rowLine = rowLines[rowIndex]
            val sourceSpan = sourceSpans.getOrNull(rowIndex)
            val cells = split(rowLine)
            val row = TableRow()
            sourceSpan?.let(row::addSourceSpan)
            for (index in 0 until headerColumnCount) {
                val cell = cells.getOrNull(index) ?: SourceLine.of("", null)
                row.appendChild(parseCell(cell, index, inlineParser))
            }
            val tableBody =
                body ?: TableBody().also {
                    body = it
                    table.appendChild(it)
                }
            tableBody.appendChild(row)
            sourceSpan?.let(tableBody::addSourceSpan)
        }
    }

    private fun parseCell(
        cell: SourceLine,
        column: Int,
        inlineParser: InlineParser,
    ): TableCell {
        val tableCell = TableCell()
        cell.sourceSpan?.let(tableCell::addSourceSpan)
        columns.getOrNull(column)?.let { info ->
            tableCell.alignment = info.alignment
            tableCell.width = info.width
        }
        val content = cell.content
        val start = Characters.skipSpaceTab(content, 0, content.length)
        val end = Characters.skipSpaceTabBackwards(content, content.length - 1, start)
        inlineParser.parse(SourceLines.of(cell.substring(start, end + 1)), tableCell)
        return tableCell
    }

    class Factory : AbstractBlockParserFactory() {
        override fun tryStart(
            state: ParserState,
            matchedBlockParser: MatchedBlockParser,
        ): BlockStart? {
            val paragraphLines = matchedBlockParser.paragraphLines.lines
            if (
                paragraphLines.isNotEmpty() &&
                Characters.find('|', paragraphLines.last().content, 0) != -1
            ) {
                val line = state.line
                val separatorLine = line.substring(state.index, line.content.length)
                val columns = parseSeparator(separatorLine.content)
                if (!columns.isNullOrEmpty()) {
                    val paragraph = paragraphLines.last()
                    val headerCells = split(paragraph)
                    if (columns.size >= headerCells.size) {
                        return BlockStart.of(TableBlockParser(columns, paragraph))
                            .atIndex(state.index)
                            .replaceParagraphLines(1)
                    }
                }
            }
            return BlockStart.none()
        }
    }

    private data class TableCellInfo(
        val alignment: TableCell.Alignment?,
        val width: Int,
    )

    companion object {
        private fun split(line: SourceLine): List<SourceLine> {
            val row = line.content
            val nonSpace = Characters.skipSpaceTab(row, 0, row.length)
            var cellStart = nonSpace
            var cellEnd = row.length
            if (nonSpace < row.length && row[nonSpace] == '|') {
                cellStart = nonSpace + 1
                val nonSpaceEnd =
                    Characters.skipSpaceTabBackwards(row, row.length - 1, cellStart)
                cellEnd = nonSpaceEnd + 1
            }
            val cells = mutableListOf<SourceLine>()
            val content = StringBuilder()
            var index = cellStart
            while (index < cellEnd) {
                when (row[index]) {
                    '\\' ->
                        if (index + 1 < cellEnd && row[index + 1] == '|') {
                            content.append('|')
                            index++
                        } else {
                            content.append('\\')
                        }
                    '|' -> {
                        cells.add(
                            SourceLine.of(
                                content.toString(),
                                line.substring(cellStart, index).sourceSpan,
                            ),
                        )
                        content.setLength(0)
                        cellStart = index + 1
                    }
                    else -> content.append(row[index])
                }
                index++
            }
            if (content.isNotEmpty()) {
                cells.add(
                    SourceLine.of(
                        content.toString(),
                        line.substring(cellStart, line.content.length).sourceSpan,
                    ),
                )
            }
            return cells
        }

        private fun parseSeparator(separator: CharSequence): List<TableCellInfo>? {
            val columns = mutableListOf<TableCellInfo>()
            var pipes = 0
            var valid = false
            var index = 0
            var width = 0
            while (index < separator.length) {
                when (val character = separator[index]) {
                    '|' -> {
                        index++
                        pipes++
                        if (pipes > 1) {
                            return null
                        }
                        valid = true
                    }
                    '-', ':' -> {
                        if (pipes == 0 && columns.isNotEmpty()) {
                            return null
                        }
                        var left = false
                        var right = false
                        if (character == ':') {
                            left = true
                            index++
                            width++
                        }
                        var haveDash = false
                        while (index < separator.length && separator[index] == '-') {
                            index++
                            width++
                            haveDash = true
                        }
                        if (!haveDash) {
                            return null
                        }
                        if (index < separator.length && separator[index] == ':') {
                            right = true
                            index++
                            width++
                        }
                        val alignment =
                            when {
                                left && right -> TableCell.Alignment.CENTER
                                left -> TableCell.Alignment.LEFT
                                right -> TableCell.Alignment.RIGHT
                                else -> null
                            }
                        columns.add(TableCellInfo(alignment, width))
                        width = 0
                        pipes = 0
                    }
                    ' ', '\t' -> index++
                    else -> return null
                }
            }
            return if (valid) columns else null
        }
    }
}
