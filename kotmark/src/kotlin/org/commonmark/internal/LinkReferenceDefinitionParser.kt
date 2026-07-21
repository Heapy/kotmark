package org.commonmark.internal

import org.commonmark.internal.util.Escaping
import org.commonmark.internal.util.LinkScanner
import org.commonmark.node.LinkReferenceDefinition
import org.commonmark.node.SourceSpan
import org.commonmark.parser.SourceLine
import org.commonmark.parser.SourceLines
import org.commonmark.parser.beta.Scanner

class LinkReferenceDefinitionParser {
    var state: State = State.START_DEFINITION
        private set

    private val paragraphLinesField = mutableListOf<SourceLine>()
    private val definitionsField = mutableListOf<LinkReferenceDefinition>()
    private val sourceSpans = mutableListOf<SourceSpan>()

    private var label: StringBuilder? = null
    private var destination: String? = null
    private var titleDelimiter: Char = Scanner.END
    private var title: StringBuilder? = null
    private var referenceValid: Boolean = false

    val paragraphLines: SourceLines
        get() = SourceLines.of(paragraphLinesField)

    val paragraphSourceSpans: List<SourceSpan>
        get() = sourceSpans

    val definitions: List<LinkReferenceDefinition>
        get() {
            finishReference()
            return definitionsField
        }

    fun parse(line: SourceLine) {
        paragraphLinesField.add(line)
        if (state == State.PARAGRAPH) {
            return
        }

        val scanner = Scanner.of(SourceLines.of(line))
        while (scanner.hasNext()) {
            val success = when (state) {
                State.START_DEFINITION -> startDefinition(scanner)
                State.LABEL -> label(scanner)
                State.DESTINATION -> destination(scanner)
                State.START_TITLE -> startTitle(scanner)
                State.TITLE -> title(scanner)
                State.PARAGRAPH -> error("Paragraph state handled before scanning")
            }
            if (!success) {
                state = State.PARAGRAPH
                finishReference()
                return
            }
        }
    }

    fun addSourceSpan(sourceSpan: SourceSpan) {
        sourceSpans.add(sourceSpan)
    }

    fun removeLines(lines: Int): List<SourceSpan> {
        val firstRemoved = (sourceSpans.size - lines).coerceAtLeast(0)
        val removedSpans = sourceSpans.subList(firstRemoved, sourceSpans.size).toList()
        removeLast(lines, paragraphLinesField)
        removeLast(lines, sourceSpans)
        return removedSpans
    }

    private fun startDefinition(scanner: Scanner): Boolean {
        finishReference()
        scanner.whitespace()
        if (!scanner.next('[')) {
            return false
        }

        state = State.LABEL
        label = StringBuilder()
        if (!scanner.hasNext()) {
            label?.append('\n')
        }
        return true
    }

    private fun label(scanner: Scanner): Boolean {
        val start = scanner.position()
        if (!LinkScanner.scanLinkLabelContent(scanner)) {
            return false
        }
        label?.append(scanner.getSource(start, scanner.position()).content)

        return when {
            !scanner.hasNext() -> {
                label?.append('\n')
                true
            }

            scanner.next(']') -> {
                if (!scanner.next(':')) {
                    return false
                }

                val labelText = label?.toString().orEmpty()
                if (
                    labelText.length > 999 ||
                    Escaping.normalizeLabelContent(labelText).isEmpty()
                ) {
                    return false
                }
                state = State.DESTINATION
                scanner.whitespace()
                true
            }

            else -> false
        }
    }

    private fun destination(scanner: Scanner): Boolean {
        scanner.whitespace()
        val start = scanner.position()
        if (!LinkScanner.scanLinkDestination(scanner)) {
            return false
        }

        val rawDestination = scanner.getSource(start, scanner.position()).content
        destination =
            if (rawDestination.startsWith("<")) {
                rawDestination.substring(1, rawDestination.lastIndex)
            } else {
                rawDestination
            }

        val whitespace = scanner.whitespace()
        if (!scanner.hasNext()) {
            referenceValid = true
            paragraphLinesField.clear()
        } else if (whitespace == 0) {
            return false
        }

        state = State.START_TITLE
        return true
    }

    private fun startTitle(scanner: Scanner): Boolean {
        scanner.whitespace()
        if (!scanner.hasNext()) {
            state = State.START_DEFINITION
            return true
        }

        titleDelimiter = when (scanner.peek()) {
            '"', '\'' -> scanner.peek()
            '(' -> ')'
            else -> Scanner.END
        }

        if (titleDelimiter != Scanner.END) {
            state = State.TITLE
            title = StringBuilder()
            scanner.next()
            if (!scanner.hasNext()) {
                title?.append('\n')
            }
        } else {
            state = State.START_DEFINITION
        }
        return true
    }

    private fun title(scanner: Scanner): Boolean {
        val start = scanner.position()
        if (!LinkScanner.scanLinkTitleContent(scanner, titleDelimiter)) {
            title = null
            return false
        }

        title?.append(scanner.getSource(start, scanner.position()).content)
        if (!scanner.hasNext()) {
            title?.append('\n')
            return true
        }

        scanner.next()
        scanner.whitespace()
        if (scanner.hasNext()) {
            title = null
            return false
        }

        referenceValid = true
        paragraphLinesField.clear()
        state = State.START_DEFINITION
        return true
    }

    private fun finishReference() {
        if (!referenceValid) {
            return
        }

        val definition = LinkReferenceDefinition(
            label?.toString(),
            Escaping.unescapeString(destination.orEmpty()),
            title?.let { Escaping.unescapeString(it.toString()) },
        )
        definition.setSourceSpans(sourceSpans)
        sourceSpans.clear()
        definitionsField.add(definition)

        label = null
        referenceValid = false
        destination = null
        title = null
    }

    enum class State {
        START_DEFINITION,
        LABEL,
        DESTINATION,
        START_TITLE,
        TITLE,
        PARAGRAPH,
    }

    companion object {
        private fun <T> removeLast(count: Int, list: MutableList<T>) {
            if (count >= list.size) {
                list.clear()
            } else {
                repeat(count) {
                    list.removeLast()
                }
            }
        }
    }
}
