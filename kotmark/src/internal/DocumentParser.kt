package io.heapy.kotmark.internal

import io.heapy.kotmark.internal.util.Parsing
import io.heapy.kotmark.node.Block
import io.heapy.kotmark.node.BlockQuote
import io.heapy.kotmark.node.Document
import io.heapy.kotmark.node.FencedCodeBlock
import io.heapy.kotmark.node.Heading
import io.heapy.kotmark.node.HtmlBlock
import io.heapy.kotmark.node.IndentedCodeBlock
import io.heapy.kotmark.node.ListBlock
import io.heapy.kotmark.node.Paragraph
import io.heapy.kotmark.node.SourceSpan
import io.heapy.kotmark.node.ThematicBreak
import io.heapy.kotmark.parser.IncludeSourceSpans
import io.heapy.kotmark.parser.InlineParserFactory
import io.heapy.kotmark.parser.SourceLine
import io.heapy.kotmark.parser.SourceLines
import io.heapy.kotmark.parser.beta.InlineContentParserFactory
import io.heapy.kotmark.parser.beta.LinkProcessor
import io.heapy.kotmark.parser.block.BlockParser
import io.heapy.kotmark.parser.block.BlockParserFactory
import io.heapy.kotmark.parser.block.MatchedBlockParser
import io.heapy.kotmark.parser.block.ParserState
import io.heapy.kotmark.parser.delimiter.DelimiterProcessor
import io.heapy.kotmark.text.Characters
import kotlin.reflect.KClass

class DocumentParser(
    private val blockParserFactories: List<BlockParserFactory>,
    private val inlineParserFactory: InlineParserFactory,
    private val inlineContentParserFactories: List<InlineContentParserFactory>,
    private val delimiterProcessors: List<DelimiterProcessor>,
    private val linkProcessors: List<LinkProcessor>,
    private val linkMarkers: Set<Char>,
    private val includeSourceSpans: IncludeSourceSpans,
    private val maxOpenBlockParsers: Int,
) : ParserState {
    private lateinit var currentLine: SourceLine
    private var lineIndex = -1
    private var currentIndex = 0
    private var currentColumn = 0
    private var columnIsInTab = false
    private var nextNonSpace = 0
    private var nextNonSpaceColumn = 0
    private var currentIndent = 0
    private var blank = false

    private val documentBlockParser = DocumentBlockParser()
    private val definitions = Definitions()
    private val openBlockParsers = mutableListOf<OpenBlockParser>()
    private val allBlockParsers = mutableListOf<BlockParser>()

    init {
        activateBlockParser(OpenBlockParser(documentBlockParser, 0))
    }

    override val line: SourceLine
        get() = currentLine

    override val index: Int
        get() = currentIndex

    override val nextNonSpaceIndex: Int
        get() = nextNonSpace

    override val column: Int
        get() = currentColumn

    override val indent: Int
        get() = currentIndent

    override val isBlank: Boolean
        get() = blank

    override val activeBlockParser: BlockParser
        get() = openBlockParsers.last().blockParser

    /**
     * Parse Markdown into a document AST.
     */
    fun parse(input: String): Document {
        var lineStart = 0
        var lineBreak = Characters.findLineBreak(input, lineStart)
        while (lineBreak != -1) {
            parseLine(input.substring(lineStart, lineBreak), lineStart)
            lineStart =
                if (
                    lineBreak + 1 < input.length &&
                    input[lineBreak] == '\r' &&
                    input[lineBreak + 1] == '\n'
                ) {
                    lineBreak + 2
                } else {
                    lineBreak + 1
                }
            lineBreak = Characters.findLineBreak(input, lineStart)
        }
        if (input.isNotEmpty() && (lineStart == 0 || lineStart < input.length)) {
            parseLine(input.substring(lineStart), lineStart)
        }
        return finalizeAndProcess()
    }

    private fun parseLine(inputLine: String, inputIndex: Int) {
        setLine(inputLine, inputIndex)

        var matches = 1
        for (i in 1..<openBlockParsers.size) {
            val openBlockParser = openBlockParsers[i]
            val blockParser = openBlockParser.blockParser
            findNextNonSpace()

            val blockContinue = blockParser.tryContinue(this) as? BlockContinueImpl ?: break
            openBlockParser.sourceIndex = index
            if (blockContinue.isFinalize) {
                addSourceSpans()
                closeBlockParsers(openBlockParsers.size - i)
                return
            }
            when {
                blockContinue.newIndex != -1 -> setNewIndex(blockContinue.newIndex)
                blockContinue.newColumn != -1 -> setNewColumn(blockContinue.newColumn)
            }
            matches++
        }

        var unmatchedBlocks = openBlockParsers.size - matches
        var blockParser = openBlockParsers[matches - 1].blockParser
        var startedNewBlock = false
        var lastIndex = index

        var tryBlockStarts = blockParser.block is Paragraph || blockParser.isContainer()
        while (tryBlockStarts) {
            lastIndex = index
            findNextNonSpace()

            if (
                isBlank ||
                (indent < Parsing.CODE_BLOCK_INDENT && Characters.isLetter(line.content, nextNonSpace))
            ) {
                setNewIndex(nextNonSpace)
                break
            }

            val blockStart = findBlockStart(blockParser)
            if (blockStart == null) {
                setNewIndex(nextNonSpace)
                break
            }

            startedNewBlock = true
            val sourceIndex = index

            if (unmatchedBlocks > 0) {
                closeBlockParsers(unmatchedBlocks)
                unmatchedBlocks = 0
            }

            when {
                blockStart.newIndex != -1 -> setNewIndex(blockStart.newIndex)
                blockStart.newColumn != -1 -> setNewColumn(blockStart.newColumn)
            }

            var replacedSourceSpans: List<SourceSpan>? = null
            if (
                blockStart.replaceParagraphLines >= 1 ||
                blockStart.isReplaceActiveBlockParser
            ) {
                val active = activeBlockParser
                if (active is ParagraphParser) {
                    val lines =
                        if (blockStart.isReplaceActiveBlockParser) {
                            Int.MAX_VALUE
                        } else {
                            blockStart.replaceParagraphLines
                        }
                    replacedSourceSpans = replaceParagraphLines(lines, active)
                } else if (blockStart.isReplaceActiveBlockParser) {
                    replacedSourceSpans = prepareActiveBlockParserForReplacement(active)
                }
            }

            for (newBlockParser in blockStart.blockParsers) {
                addChild(OpenBlockParser(newBlockParser, sourceIndex))
                replacedSourceSpans?.let(newBlockParser.block::setSourceSpans)
                blockParser = newBlockParser
                tryBlockStarts = newBlockParser.isContainer()
            }
        }

        if (
            !startedNewBlock &&
            !isBlank &&
            activeBlockParser.canHaveLazyContinuationLines()
        ) {
            openBlockParsers.last().sourceIndex = lastIndex
            addLine()
        } else {
            if (unmatchedBlocks > 0) {
                closeBlockParsers(unmatchedBlocks)
            }

            when {
                !blockParser.isContainer() -> addLine()
                !isBlank -> {
                    val paragraphParser = ParagraphParser()
                    addChild(OpenBlockParser(paragraphParser, lastIndex))
                    addLine()
                }
                else -> addSourceSpans()
            }
        }
    }

    private fun setLine(inputLine: String, inputIndex: Int) {
        lineIndex++
        currentIndex = 0
        currentColumn = 0
        columnIsInTab = false

        val lineContent = prepareLine(inputLine)
        val sourceSpan =
            if (includeSourceSpans != IncludeSourceSpans.NONE) {
                SourceSpan.of(lineIndex, 0, inputIndex, lineContent.length)
            } else {
                null
            }
        currentLine = SourceLine.of(lineContent, sourceSpan)
    }

    private fun findNextNonSpace() {
        var i = index
        var columns = column

        blank = true
        while (i < line.content.length) {
            when (line.content[i]) {
                ' ' -> {
                    i++
                    columns++
                }
                '\t' -> {
                    i++
                    columns += 4 - columns % 4
                }
                else -> {
                    blank = false
                    break
                }
            }
        }

        nextNonSpace = i
        nextNonSpaceColumn = columns
        currentIndent = nextNonSpaceColumn - column
    }

    private fun setNewIndex(newIndex: Int) {
        if (newIndex >= nextNonSpace) {
            currentIndex = nextNonSpace
            currentColumn = nextNonSpaceColumn
        }
        while (index < newIndex && index != line.content.length) {
            advance()
        }
        columnIsInTab = false
    }

    private fun setNewColumn(newColumn: Int) {
        if (newColumn >= nextNonSpaceColumn) {
            currentIndex = nextNonSpace
            currentColumn = nextNonSpaceColumn
        }
        while (column < newColumn && index != line.content.length) {
            advance()
        }
        if (column > newColumn) {
            currentIndex--
            currentColumn = newColumn
            columnIsInTab = true
        } else {
            columnIsInTab = false
        }
    }

    private fun advance() {
        val character = line.content[index]
        currentIndex++
        currentColumn +=
            if (character == '\t') {
                Parsing.columnsToNextTabStop(column)
            } else {
                1
            }
    }

    private fun addLine() {
        val content =
            when {
                columnIsInTab -> {
                    val afterTab = index + 1
                    val rest = line.content.subSequence(afterTab, line.content.length)
                    buildString(Parsing.columnsToNextTabStop(column) + rest.length) {
                        repeat(Parsing.columnsToNextTabStop(column)) {
                            append(' ')
                        }
                        append(rest)
                    }
                }
                index == 0 -> line.content
                else -> line.content.subSequence(index, line.content.length)
            }
        val sourceSpan =
            if (
                includeSourceSpans == IncludeSourceSpans.BLOCKS_AND_INLINES &&
                index < requireNotNull(line.sourceSpan).length
            ) {
                requireNotNull(line.sourceSpan).subSpan(index)
            } else {
                null
            }
        activeBlockParser.addLine(SourceLine.of(content, sourceSpan))
        addSourceSpans()
    }

    private fun addSourceSpans() {
        if (includeSourceSpans == IncludeSourceSpans.NONE) {
            return
        }
        val sourceSpan = requireNotNull(line.sourceSpan)
        for (i in 1..<openBlockParsers.size) {
            val openBlockParser = openBlockParsers[i]
            val blockIndex = minOf(openBlockParser.sourceIndex, index)
            val length = line.content.length - blockIndex
            if (length != 0) {
                openBlockParser.blockParser.addSourceSpan(sourceSpan.subSpan(blockIndex))
            }
        }
    }

    private fun findBlockStart(blockParser: BlockParser): BlockStartImpl? {
        if (openBlockParsers.size > maxOpenBlockParsers) {
            return null
        }
        val matchedBlockParser = MatchedBlockParserImpl(blockParser)
        for (factory in blockParserFactories) {
            val result = factory.tryStart(this, matchedBlockParser)
            if (result is BlockStartImpl) {
                return result
            }
        }
        return null
    }

    private fun processInlines() {
        val context =
            InlineParserContextImpl(
                inlineContentParserFactories,
                delimiterProcessors,
                linkProcessors,
                linkMarkers,
                definitions,
            )
        val inlineParser = inlineParserFactory.create(context)
        for (blockParser in allBlockParsers) {
            blockParser.parseInlines(inlineParser)
        }
    }

    private fun addChild(openBlockParser: OpenBlockParser) {
        while (!activeBlockParser.canContain(openBlockParser.blockParser.block)) {
            closeBlockParsers(1)
        }
        activeBlockParser.block.appendChild(openBlockParser.blockParser.block)
        activateBlockParser(openBlockParser)
    }

    private fun activateBlockParser(openBlockParser: OpenBlockParser) {
        openBlockParsers.add(openBlockParser)
    }

    private fun deactivateBlockParser(): OpenBlockParser =
        openBlockParsers.removeAt(openBlockParsers.lastIndex)

    private fun replaceParagraphLines(
        lines: Int,
        paragraphParser: ParagraphParser,
    ): List<SourceSpan> {
        val sourceSpans = paragraphParser.removeLines(lines)
        closeBlockParsers(1)
        return sourceSpans
    }

    private fun prepareActiveBlockParserForReplacement(blockParser: BlockParser): List<SourceSpan> {
        val _ = deactivateBlockParser()
        blockParser.closeBlock()
        blockParser.block.unlink()
        return blockParser.block.getSourceSpans()
    }

    private fun finalizeAndProcess(): Document {
        closeBlockParsers(openBlockParsers.size)
        processInlines()
        return documentBlockParser.block
    }

    private fun closeBlockParsers(count: Int) {
        repeat(count) {
            val blockParser = deactivateBlockParser().blockParser
            finalize(blockParser)
            allBlockParsers.add(blockParser)
        }
    }

    private fun finalize(blockParser: BlockParser) {
        addDefinitionsFrom(blockParser)
        blockParser.closeBlock()
    }

    private fun addDefinitionsFrom(blockParser: BlockParser) {
        for (definitionMap in blockParser.definitions) {
            definitions.addDefinitions(definitionMap)
        }
    }

    private class MatchedBlockParserImpl(
        override val matchedBlockParser: BlockParser,
    ) : MatchedBlockParser {
        override val paragraphLines: SourceLines
            get() =
                if (matchedBlockParser is ParagraphParser) {
                    matchedBlockParser.paragraphLines
                } else {
                    SourceLines.empty()
                }
    }

    private class OpenBlockParser(
        val blockParser: BlockParser,
        var sourceIndex: Int,
    )

    companion object {
        private val coreFactoryTypes: Set<KClass<out Block>> =
            linkedSetOf(
                BlockQuote::class,
                Heading::class,
                FencedCodeBlock::class,
                HtmlBlock::class,
                ThematicBreak::class,
                ListBlock::class,
                IndentedCodeBlock::class,
            )

        private val nodesToCoreFactories: Map<KClass<out Block>, BlockParserFactory> =
            mapOf(
                BlockQuote::class to BlockQuoteParser.Factory(),
                Heading::class to HeadingParser.Factory(),
                FencedCodeBlock::class to FencedCodeBlockParser.Factory(),
                HtmlBlock::class to HtmlBlockParser.Factory(),
                ThematicBreak::class to ThematicBreakParser.Factory(),
                ListBlock::class to ListBlockParser.Factory(),
                IndentedCodeBlock::class to IndentedCodeBlockParser.Factory(),
            )

        fun getDefaultBlockParserTypes(): Set<KClass<out Block>> = coreFactoryTypes

        fun calculateBlockParserFactories(
            customBlockParserFactories: List<BlockParserFactory>,
            enabledBlockTypes: Set<KClass<out Block>>,
        ): List<BlockParserFactory> =
            buildList {
                addAll(customBlockParserFactories)
                for (blockType in enabledBlockTypes) {
                    add(requireNotNull(nodesToCoreFactories[blockType]))
                }
            }

        fun checkEnabledBlockTypes(enabledBlockTypes: Set<KClass<out Block>>) {
            for (enabledBlockType in enabledBlockTypes) {
                require(enabledBlockType in nodesToCoreFactories) {
                    "Can't enable block type $enabledBlockType, possible options are: ${nodesToCoreFactories.keys}"
                }
            }
        }

        private fun prepareLine(line: String): String =
            if ('\u0000' in line) {
                line.replace('\u0000', '\uFFFD')
            } else {
                line
            }
    }
}
