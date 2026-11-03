package io.heapy.kotmark.internal

import io.heapy.kotmark.internal.inline.AsteriskDelimiterProcessor
import io.heapy.kotmark.internal.inline.AutolinkInlineParser
import io.heapy.kotmark.internal.inline.BackslashInlineParser
import io.heapy.kotmark.internal.inline.BackticksInlineParser
import io.heapy.kotmark.internal.inline.CoreLinkProcessor
import io.heapy.kotmark.internal.inline.EntityInlineParser
import io.heapy.kotmark.internal.inline.HtmlInlineParser
import io.heapy.kotmark.internal.inline.LinkResultImpl
import io.heapy.kotmark.internal.inline.ParsedInlineImpl
import io.heapy.kotmark.internal.inline.UnderscoreDelimiterProcessor
import io.heapy.kotmark.internal.util.Escaping
import io.heapy.kotmark.internal.util.LinkScanner
import io.heapy.kotmark.node.HardLineBreak
import io.heapy.kotmark.node.Node
import io.heapy.kotmark.node.SoftLineBreak
import io.heapy.kotmark.node.SourceSpans
import io.heapy.kotmark.node.Text
import io.heapy.kotmark.parser.InlineParser
import io.heapy.kotmark.parser.InlineParserContext
import io.heapy.kotmark.parser.SourceLines
import io.heapy.kotmark.parser.beta.InlineContentParser
import io.heapy.kotmark.parser.beta.InlineContentParserFactory
import io.heapy.kotmark.parser.beta.InlineParserState
import io.heapy.kotmark.parser.beta.LinkInfo
import io.heapy.kotmark.parser.beta.LinkProcessor
import io.heapy.kotmark.parser.beta.Position
import io.heapy.kotmark.parser.beta.Scanner
import io.heapy.kotmark.parser.delimiter.DelimiterProcessor
import io.heapy.kotmark.text.Characters

class InlineParserImpl(
    private val context: InlineParserContext,
) : InlineParser, InlineParserState {
    private val inlineContentParserFactories: List<InlineContentParserFactory> =
        calculateInlineContentParserFactories(context.customInlineContentParserFactories)
    private val delimiterProcessors: Map<Char, DelimiterProcessor> =
        calculateDelimiterProcessors(context.customDelimiterProcessors)
    private val linkProcessors: List<LinkProcessor> =
        calculateLinkProcessors(context.customLinkProcessors)
    private val linkMarkers: Set<Char> =
        context.customLinkMarkers + '!'
    private val specialCharacters: Set<Char> =
        calculateSpecialCharacters(linkMarkers, delimiterProcessors.keys, inlineContentParserFactories)

    private lateinit var inlineParsers: Map<Char, List<InlineContentParser>>
    private lateinit var scannerValue: Scanner
    private var includeSourceSpans: Boolean = false
    private var trailingSpaces: Int = 0
    private var lastDelimiter: Delimiter? = null
    private var lastBracket: Bracket? = null

    override fun scanner(): Scanner = scannerValue

    override fun parse(lines: SourceLines, node: Node) {
        reset(lines)
        while (true) {
            val parsedNodes = parseInline() ?: break
            for (parsedNode in parsedNodes) {
                node.appendChild(parsedNode)
            }
        }
        processDelimiters(null)
        mergeChildTextNodes(node)
    }

    internal fun reset(lines: SourceLines) {
        scannerValue = Scanner.of(lines)
        includeSourceSpans = lines.sourceSpans.isNotEmpty()
        trailingSpaces = 0
        lastDelimiter = null
        lastBracket = null
        inlineParsers = createInlineContentParsers()
    }

    private fun text(sourceLines: SourceLines): Text =
        Text(sourceLines.content).apply {
            setSourceSpans(sourceLines.sourceSpans)
        }

    private fun parseInline(): List<Node>? {
        val current = scannerValue.peek()
        when (current) {
            '[' -> return listOf(parseOpenBracket())
            ']' -> return listOf(parseCloseBracket())
            '\n' -> return listOf(parseLineBreak())
            Scanner.END -> return null
        }

        if (current in linkMarkers) {
            val markerPosition = scannerValue.position()
            val nodes = parseLinkMarker()
            if (nodes != null) {
                return nodes
            }
            scannerValue.setPosition(markerPosition)
        }

        if (current !in specialCharacters) {
            return listOf(parseText())
        }

        inlineParsers[current]?.let { parsers ->
            val position = scannerValue.position()
            for (inlineParser in parsers) {
                val parsedInline = inlineParser.tryParse(this)
                if (parsedInline is ParsedInlineImpl) {
                    val node = parsedInline.node
                    scannerValue.setPosition(parsedInline.position)
                    if (includeSourceSpans && node.getSourceSpans().isEmpty()) {
                        node.setSourceSpans(
                            scannerValue.getSource(position, scannerValue.position()).sourceSpans,
                        )
                    }
                    return listOf(node)
                }
                scannerValue.setPosition(position)
            }
        }

        delimiterProcessors[current]?.let { delimiterProcessor ->
            parseDelimiters(delimiterProcessor, current)?.let { return it }
        }

        return listOf(parseText())
    }

    private fun parseDelimiters(
        delimiterProcessor: DelimiterProcessor,
        delimiterChar: Char,
    ): List<Node>? {
        val result = scanDelimiters(delimiterProcessor, delimiterChar) ?: return null
        val delimiter = Delimiter(
            result.characters,
            delimiterChar,
            result.canOpen,
            result.canClose,
            lastDelimiter,
        )
        delimiter.previous?.next = delimiter
        lastDelimiter = delimiter
        return result.characters
    }

    private fun parseOpenBracket(): Node {
        val start = scannerValue.position()
        scannerValue.next()
        val contentPosition = scannerValue.position()
        val node = text(scannerValue.getSource(start, contentPosition))
        addBracket(
            Bracket.link(node, start, contentPosition, lastBracket, lastDelimiter),
        )
        return node
    }

    private fun parseLinkMarker(): List<Node>? {
        val markerPosition = scannerValue.position()
        scannerValue.next()
        val bracketPosition = scannerValue.position()
        if (!scannerValue.next('[')) {
            return null
        }

        val contentPosition = scannerValue.position()
        val markerNode = text(scannerValue.getSource(markerPosition, bracketPosition))
        val bracketNode = text(scannerValue.getSource(bracketPosition, contentPosition))
        addBracket(
            Bracket.withMarker(
                markerNode,
                markerPosition,
                bracketNode,
                bracketPosition,
                contentPosition,
                lastBracket,
                lastDelimiter,
            ),
        )
        return listOf(markerNode, bracketNode)
    }

    private fun parseCloseBracket(): Node {
        val beforeClose = scannerValue.position()
        scannerValue.next()
        val afterClose = scannerValue.position()

        val opener = lastBracket
            ?: return text(scannerValue.getSource(beforeClose, afterClose))

        if (!opener.allowed) {
            removeLastBracket()
            return text(scannerValue.getSource(beforeClose, afterClose))
        }

        parseLinkOrImage(opener, beforeClose)?.let { return it }
        scannerValue.setPosition(afterClose)
        removeLastBracket()
        return text(scannerValue.getSource(beforeClose, afterClose))
    }

    private fun parseLinkOrImage(opener: Bracket, beforeClose: Position): Node? {
        val linkInfo = parseLinkInfo(opener, beforeClose) ?: return null
        val processorStartPosition = scannerValue.position()

        for (linkProcessor in linkProcessors) {
            val linkResult = linkProcessor.process(linkInfo, scannerValue, context)
            if (linkResult !is LinkResultImpl) {
                scannerValue.setPosition(processorStartPosition)
                continue
            }

            scannerValue.setPosition(linkResult.position)
            return when (linkResult.type) {
                LinkResultImpl.Type.WRAP ->
                    wrapBracket(opener, linkResult.node, linkResult.isIncludeMarker)

                LinkResultImpl.Type.REPLACE ->
                    replaceBracket(opener, linkResult.node, linkResult.isIncludeMarker)
            }
        }
        return null
    }

    private fun parseLinkInfo(opener: Bracket, beforeClose: Position): LinkInfo? {
        val afterClose = scannerValue.position()
        val destinationTitle = parseInlineDestinationTitle(scannerValue)
        if (destinationTitle != null) {
            val text = scannerValue.getSource(opener.contentPosition, beforeClose).content
            return LinkInfoImpl(
                opener.markerNode,
                opener.bracketNode,
                text,
                null,
                destinationTitle.destination,
                destinationTitle.title,
                afterClose,
            )
        }
        scannerValue.setPosition(afterClose)

        val label = parseLinkLabel(scannerValue)
        if (label == null) {
            scannerValue.setPosition(afterClose)
        }
        val textIsReference = label.isNullOrEmpty()
        if (opener.bracketAfter && textIsReference && opener.markerNode == null) {
            return null
        }

        val text = scannerValue.getSource(opener.contentPosition, beforeClose).content
        return LinkInfoImpl(
            opener.markerNode,
            opener.bracketNode,
            text,
            label,
            null,
            null,
            afterClose,
        )
    }

    private fun wrapBracket(
        opener: Bracket,
        wrapperNode: Node,
        includeMarker: Boolean,
    ): Node {
        var node = opener.bracketNode.next
        while (node != null) {
            val next = node.next
            wrapperNode.appendChild(node)
            node = next
        }

        if (includeSourceSpans) {
            val startPosition =
                if (includeMarker && opener.markerPosition != null) {
                    opener.markerPosition
                } else {
                    opener.bracketPosition
                }
            wrapperNode.setSourceSpans(
                scannerValue.getSource(startPosition, scannerValue.position()).sourceSpans,
            )
        }

        processDelimiters(opener.previousDelimiter)
        mergeChildTextNodes(wrapperNode)
        if (includeMarker) {
            opener.markerNode?.unlink()
        }
        opener.bracketNode.unlink()
        removeLastBracket()

        if (opener.markerNode == null) {
            disallowPreviousLinks()
        }
        return wrapperNode
    }

    private fun replaceBracket(
        opener: Bracket,
        replacement: Node,
        includeMarker: Boolean,
    ): Node {
        while (lastDelimiter != null && lastDelimiter !== opener.previousDelimiter) {
            removeDelimiterKeepNode(requireNotNull(lastDelimiter))
        }

        if (includeSourceSpans) {
            val startPosition =
                if (includeMarker && opener.markerPosition != null) {
                    opener.markerPosition
                } else {
                    opener.bracketPosition
                }
            replacement.setSourceSpans(
                scannerValue.getSource(startPosition, scannerValue.position()).sourceSpans,
            )
        }

        removeLastBracket()

        var node: Node? =
            if (includeMarker && opener.markerNode != null) opener.markerNode else opener.bracketNode
        while (node != null) {
            val next = node.next
            node.unlink()
            node = next
        }

        if (opener.markerNode == null || !includeMarker) {
            disallowPreviousLinks()
        }
        return replacement
    }

    private fun addBracket(bracket: Bracket) {
        lastBracket?.bracketAfter = true
        lastBracket = bracket
    }

    private fun removeLastBracket() {
        lastBracket = lastBracket?.previous
    }

    private fun disallowPreviousLinks() {
        var bracket = lastBracket
        while (bracket != null) {
            if (bracket.markerNode == null) {
                bracket.allowed = false
            }
            bracket = bracket.previous
        }
    }

    private fun parseLineBreak(): Node {
        scannerValue.next()
        val hard = trailingSpaces >= 2
        trailingSpaces = 0
        return if (hard) HardLineBreak() else SoftLineBreak()
    }

    private fun parseText(): Node {
        val start = scannerValue.position()
        scannerValue.next()
        var current: Char
        while (true) {
            current = scannerValue.peek()
            if (current == Scanner.END || current in specialCharacters) {
                break
            }
            scannerValue.next()
        }

        val source = scannerValue.getSource(start, scannerValue.position())
        var content = source.content
        when (current) {
            '\n' -> {
                val end = Characters.skipBackwards(' ', content, content.lastIndex, 0) + 1
                trailingSpaces = content.length - end
                content = content.substring(0, end)
            }

            Scanner.END -> {
                val end = Characters.skipSpaceTabBackwards(content, content.lastIndex, 0) + 1
                content = content.substring(0, end)
            }
        }

        return Text(content).apply {
            setSourceSpans(source.sourceSpans)
        }
    }

    private fun scanDelimiters(
        delimiterProcessor: DelimiterProcessor,
        delimiterChar: Char,
    ): DelimiterData? {
        val before = scannerValue.peekPreviousCodePoint()
        val start = scannerValue.position()

        val delimiterCount = scannerValue.matchMultiple(delimiterChar)
        if (delimiterCount < delimiterProcessor.minLength) {
            scannerValue.setPosition(start)
            return null
        }

        val delimiters = mutableListOf<Text>()
        scannerValue.setPosition(start)
        var positionBefore = start
        while (scannerValue.next(delimiterChar)) {
            delimiters.add(text(scannerValue.getSource(positionBefore, scannerValue.position())))
            positionBefore = scannerValue.position()
        }

        val after = scannerValue.peekCodePoint()
        val beforeIsPunctuation =
            before == Scanner.END.code || Characters.isPunctuationCodePoint(before)
        val beforeIsWhitespace =
            before == Scanner.END.code || Characters.isWhitespaceCodePoint(before)
        val afterIsPunctuation =
            after == Scanner.END.code || Characters.isPunctuationCodePoint(after)
        val afterIsWhitespace =
            after == Scanner.END.code || Characters.isWhitespaceCodePoint(after)

        val leftFlanking =
            !afterIsWhitespace && (!afterIsPunctuation || beforeIsWhitespace || beforeIsPunctuation)
        val rightFlanking =
            !beforeIsWhitespace && (!beforeIsPunctuation || afterIsWhitespace || afterIsPunctuation)

        val canOpen: Boolean
        val canClose: Boolean
        if (delimiterChar == '_') {
            canOpen = leftFlanking && (!rightFlanking || beforeIsPunctuation)
            canClose = rightFlanking && (!leftFlanking || afterIsPunctuation)
        } else {
            canOpen = leftFlanking && delimiterChar == delimiterProcessor.openingCharacter
            canClose = rightFlanking && delimiterChar == delimiterProcessor.closingCharacter
        }
        return DelimiterData(delimiters, canOpen, canClose)
    }

    private fun processDelimiters(stackBottom: Delimiter?) {
        val openersBottom = mutableMapOf<Char, Delimiter?>()

        var closer = lastDelimiter
        while (closer != null && closer.previous !== stackBottom) {
            closer = closer.previous
        }

        while (closer != null) {
            val currentCloser = closer
            val delimiterChar = currentCloser.delimiterChar
            val delimiterProcessor = delimiterProcessors[delimiterChar]
            if (!currentCloser.canClose() || delimiterProcessor == null) {
                closer = currentCloser.next
                continue
            }

            val openingDelimiterChar = delimiterProcessor.openingCharacter
            var usedDelimiters = 0
            var openerFound = false
            var potentialOpenerFound = false
            var opener = currentCloser.previous
            while (
                opener != null &&
                opener !== stackBottom &&
                opener !== openersBottom[delimiterChar]
            ) {
                if (opener.canOpen() && opener.delimiterChar == openingDelimiterChar) {
                    potentialOpenerFound = true
                    usedDelimiters = delimiterProcessor.process(opener, currentCloser)
                    if (usedDelimiters > 0) {
                        openerFound = true
                        break
                    }
                }
                opener = opener.previous
            }

            if (!openerFound) {
                if (!potentialOpenerFound) {
                    openersBottom[delimiterChar] = currentCloser.previous
                    if (!currentCloser.canOpen()) {
                        removeDelimiterKeepNode(currentCloser)
                    }
                }
                closer = currentCloser.next
                continue
            }

            val foundOpener = requireNotNull(opener)
            repeat(usedDelimiters) {
                foundOpener.characters.removeAt(foundOpener.characters.lastIndex).unlink()
            }
            repeat(usedDelimiters) {
                currentCloser.characters.removeAt(0).unlink()
            }

            removeDelimitersBetween(foundOpener, currentCloser)
            if (foundOpener.length() == 0) {
                removeDelimiterAndNodes(foundOpener)
            }
            if (currentCloser.length() == 0) {
                val next = currentCloser.next
                removeDelimiterAndNodes(currentCloser)
                closer = next
            }
        }

        while (lastDelimiter != null && lastDelimiter !== stackBottom) {
            removeDelimiterKeepNode(requireNotNull(lastDelimiter))
        }
    }

    private fun removeDelimitersBetween(opener: Delimiter, closer: Delimiter) {
        var delimiter = closer.previous
        while (delimiter != null && delimiter !== opener) {
            val previousDelimiter = delimiter.previous
            removeDelimiterKeepNode(delimiter)
            delimiter = previousDelimiter
        }
    }

    private fun removeDelimiterAndNodes(delimiter: Delimiter) {
        removeDelimiter(delimiter)
    }

    private fun removeDelimiterKeepNode(delimiter: Delimiter) {
        removeDelimiter(delimiter)
    }

    private fun removeDelimiter(delimiter: Delimiter) {
        delimiter.previous?.next = delimiter.next
        if (delimiter.next == null) {
            lastDelimiter = delimiter.previous
        } else {
            delimiter.next?.previous = delimiter.previous
        }
    }

    private fun mergeChildTextNodes(node: Node) {
        val firstChild = node.firstChild ?: return
        mergeTextNodesInclusive(firstChild, requireNotNull(node.lastChild))
    }

    private fun mergeTextNodesInclusive(fromNode: Node, toNode: Node) {
        var first: Text? = null
        var last: Text? = null
        var length = 0

        var node: Node? = fromNode
        while (node != null) {
            if (node is Text) {
                if (first == null) {
                    first = node
                }
                length += node.literal.orEmpty().length
                last = node
            } else {
                mergeIfNeeded(first, last, length)
                first = null
                last = null
                length = 0
                mergeChildTextNodes(node)
            }
            if (node === toNode) {
                break
            }
            node = node.next
        }
        mergeIfNeeded(first, last, length)
    }

    private fun mergeIfNeeded(first: Text?, last: Text?, textLength: Int) {
        if (first == null || last == null || first === last) {
            return
        }

        val builder = StringBuilder(textLength)
        builder.append(first.literal)
        val sourceSpans =
            if (includeSourceSpans) {
                SourceSpans.empty().apply { addAll(first.getSourceSpans()) }
            } else {
                null
            }

        var node = first.next
        val stop = last.next
        while (node !== stop) {
            val text = node as Text
            builder.append(text.literal)
            sourceSpans?.addAll(text.getSourceSpans())
            node = text.next
            text.unlink()
        }

        first.literal = builder.toString()
        if (sourceSpans != null) {
            first.setSourceSpans(sourceSpans.getSourceSpans())
        }
    }

    private fun createInlineContentParsers(): Map<Char, List<InlineContentParser>> {
        val result = mutableMapOf<Char, MutableList<InlineContentParser>>()
        for (factory in inlineContentParserFactories) {
            val parser = factory.create()
            for (trigger in factory.triggerCharacters) {
                result.getOrPut(trigger) { mutableListOf() }.add(parser)
            }
        }
        return result
    }

    private data class DelimiterData(
        val characters: MutableList<Text>,
        val canOpen: Boolean,
        val canClose: Boolean,
    )

    private data class DestinationTitle(
        val destination: String,
        val title: String?,
    )

    private class LinkInfoImpl(
        private val marker: Text?,
        private val openingBracket: Text,
        private val text: String,
        private val label: String?,
        private val destination: String?,
        private val title: String?,
        private val afterTextBracket: Position,
    ) : LinkInfo {
        override fun marker(): Text? = marker

        override fun openingBracket(): Text = openingBracket

        override fun text(): String = text

        override fun label(): String? = label

        override fun destination(): String? = destination

        override fun title(): String? = title

        override fun afterTextBracket(): Position = afterTextBracket
    }

    companion object {
        private fun calculateInlineContentParserFactories(
            customFactories: List<InlineContentParserFactory>,
        ): List<InlineContentParserFactory> = buildList {
            addAll(customFactories)
            add(BackslashInlineParser.Factory())
            add(BackticksInlineParser.Factory())
            add(EntityInlineParser.Factory())
            add(AutolinkInlineParser.Factory())
            add(HtmlInlineParser.Factory())
        }

        private fun calculateLinkProcessors(
            customProcessors: List<LinkProcessor>,
        ): List<LinkProcessor> = buildList {
            addAll(customProcessors)
            add(CoreLinkProcessor())
        }

        private fun calculateDelimiterProcessors(
            customProcessors: List<DelimiterProcessor>,
        ): Map<Char, DelimiterProcessor> = mutableMapOf<Char, DelimiterProcessor>().apply {
            addDelimiterProcessors(
                listOf(AsteriskDelimiterProcessor(), UnderscoreDelimiterProcessor()),
                this,
            )
            addDelimiterProcessors(customProcessors, this)
        }

        private fun addDelimiterProcessors(
            processors: Iterable<DelimiterProcessor>,
            result: MutableMap<Char, DelimiterProcessor>,
        ) {
            for (processor in processors) {
                val opening = processor.openingCharacter
                val closing = processor.closingCharacter
                if (opening == closing) {
                    val old = result[opening]
                    if (old != null && old.openingCharacter == old.closingCharacter) {
                        val staggered =
                            if (old is StaggeredDelimiterProcessor) {
                                old
                            } else {
                                StaggeredDelimiterProcessor(opening).apply { add(old) }
                            }
                        staggered.add(processor)
                        result[opening] = staggered
                    } else {
                        addDelimiterProcessorForChar(opening, processor, result)
                    }
                } else {
                    addDelimiterProcessorForChar(opening, processor, result)
                    addDelimiterProcessorForChar(closing, processor, result)
                }
            }
        }

        private fun addDelimiterProcessorForChar(
            delimiterChar: Char,
            processor: DelimiterProcessor,
            result: MutableMap<Char, DelimiterProcessor>,
        ) {
            val existing = result.put(delimiterChar, processor)
            require(existing == null) {
                "Delimiter processor conflict with delimiter char '$delimiterChar'"
            }
        }

        private fun calculateSpecialCharacters(
            linkMarkers: Set<Char>,
            delimiterCharacters: Set<Char>,
            factories: List<InlineContentParserFactory>,
        ): Set<Char> = buildSet {
            addAll(linkMarkers)
            addAll(delimiterCharacters)
            for (factory in factories) {
                addAll(factory.triggerCharacters)
            }
            add('[')
            add(']')
            add('!')
            add('\n')
        }

        private fun parseInlineDestinationTitle(scanner: Scanner): DestinationTitle? {
            if (!scanner.next('(')) {
                return null
            }

            scanner.whitespace()
            val destination = parseLinkDestination(scanner) ?: return null
            var title: String? = null
            if (scanner.whitespace() >= 1) {
                title = parseLinkTitle(scanner)
                scanner.whitespace()
            }
            if (!scanner.next(')')) {
                return null
            }
            return DestinationTitle(destination, title)
        }

        private fun parseLinkDestination(scanner: Scanner): String? {
            val delimiter = scanner.peek()
            val start = scanner.position()
            if (!LinkScanner.scanLinkDestination(scanner)) {
                return null
            }

            val rawDestination = scanner.getSource(start, scanner.position()).content
            val destination =
                if (delimiter == '<') {
                    rawDestination.substring(1, rawDestination.lastIndex)
                } else {
                    rawDestination
                }
            return Escaping.unescapeString(destination)
        }

        private fun parseLinkTitle(scanner: Scanner): String? {
            val start = scanner.position()
            if (!LinkScanner.scanLinkTitle(scanner)) {
                return null
            }

            val rawTitle = scanner.getSource(start, scanner.position()).content
            return Escaping.unescapeString(rawTitle.substring(1, rawTitle.lastIndex))
        }

        internal fun parseLinkLabel(scanner: Scanner): String? {
            if (!scanner.next('[')) {
                return null
            }

            val start = scanner.position()
            if (!LinkScanner.scanLinkLabelContent(scanner)) {
                return null
            }
            val end = scanner.position()
            if (!scanner.next(']')) {
                return null
            }

            val content = scanner.getSource(start, end).content
            return content.takeIf { it.length <= 999 }
        }
    }
}
