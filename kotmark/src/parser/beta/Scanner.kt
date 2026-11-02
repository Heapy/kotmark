package io.heapy.kotmark.parser.beta

import io.heapy.kotmark.node.SourceSpan
import io.heapy.kotmark.parser.SourceLine
import io.heapy.kotmark.parser.SourceLines
import io.heapy.kotmark.text.CharMatcher
import kotlin.jvm.JvmStatic

class Scanner(
    private val lines: List<SourceLine>,
    private var lineIndex: Int,
    private var index: Int,
) {
    private var line: SourceLine = SourceLine.of("", null)
    private var lineLength: Int = 0

    init {
        if (lines.isNotEmpty()) {
            checkPosition(lineIndex, index)
            setLine(lines[lineIndex])
        }
    }

    fun peek(): Char =
        if (index < lineLength) {
            line.content[index]
        } else if (lineIndex < lines.lastIndex) {
            '\n'
        } else {
            END
        }

    fun peekCodePoint(): Int {
        if (index >= lineLength) {
            return if (lineIndex < lines.lastIndex) '\n'.code else END.code
        }

        val high = line.content[index]
        if (high.isHighSurrogate() && index + 1 < lineLength) {
            val low = line.content[index + 1]
            if (low.isLowSurrogate()) {
                return toCodePoint(high, low)
            }
        }
        return high.code
    }

    fun peekPreviousCodePoint(): Int {
        if (index <= 0) {
            return if (lineIndex > 0) '\n'.code else END.code
        }

        val previousIndex = index - 1
        val low = line.content[previousIndex]
        if (low.isLowSurrogate() && previousIndex > 0) {
            val high = line.content[previousIndex - 1]
            if (high.isHighSurrogate()) {
                return toCodePoint(high, low)
            }
        }
        return low.code
    }

    fun hasNext(): Boolean =
        index < lineLength || lineIndex < lines.lastIndex

    fun next() {
        index++
        if (index > lineLength) {
            lineIndex++
            setLine(if (lineIndex < lines.size) lines[lineIndex] else SourceLine.of("", null))
            index = 0
        }
    }

    fun next(c: Char): Boolean {
        if (peek() != c) {
            return false
        }
        next()
        return true
    }

    fun next(content: String): Boolean {
        if (index >= lineLength || index + content.length > lineLength) {
            return false
        }
        for (contentIndex in content.indices) {
            if (line.content[index + contentIndex] != content[contentIndex]) {
                return false
            }
        }
        index += content.length
        return true
    }

    /** Consumes consecutive occurrences of [c], returning the number consumed. */
    @IgnorableReturnValue
    fun matchMultiple(c: Char): Int {
        var count = 0
        while (peek() == c) {
            count++
            next()
        }
        return count
    }

    /** Consumes matching characters, returning the number consumed. */
    @IgnorableReturnValue
    fun match(matcher: CharMatcher): Int {
        var count = 0
        while (matcher.matches(peek())) {
            count++
            next()
        }
        return count
    }

    /** Consumes whitespace, returning the number of characters consumed. */
    @IgnorableReturnValue
    fun whitespace(): Int {
        var count = 0
        while (peek() in WHITESPACE) {
            count++
            next()
        }
        return count
    }

    fun find(c: Char): Int {
        var count = 0
        while (true) {
            val current = peek()
            when (current) {
                END -> return -1
                c -> return count
                else -> {
                    count++
                    next()
                }
            }
        }
    }

    fun find(matcher: CharMatcher): Int {
        var count = 0
        while (true) {
            val current = peek()
            when {
                current == END -> return -1
                matcher.matches(current) -> return count
                else -> {
                    count++
                    next()
                }
            }
        }
    }

    fun position(): Position = Position(lineIndex, index)

    fun setPosition(position: Position) {
        checkPosition(position.lineIndex, position.index)
        lineIndex = position.lineIndex
        index = position.index
        setLine(lines[lineIndex])
    }

    fun getSource(begin: Position, end: Position): SourceLines {
        if (begin.lineIndex == end.lineIndex) {
            val sourceLine = lines[begin.lineIndex]
            val newContent = sourceLine.content.subSequence(begin.index, end.index)
            val newSourceSpan: SourceSpan? =
                sourceLine.sourceSpan?.subSpan(begin.index, end.index)
            return SourceLines.of(SourceLine.of(newContent, newSourceSpan))
        }

        return SourceLines.empty().apply {
            val firstLine = this@Scanner.lines[begin.lineIndex]
            addLine(firstLine.substring(begin.index, firstLine.content.length))

            for (currentLineIndex in begin.lineIndex + 1..<end.lineIndex) {
                addLine(this@Scanner.lines[currentLineIndex])
            }

            val lastLine = this@Scanner.lines[end.lineIndex]
            addLine(lastLine.substring(0, end.index))
        }
    }

    private fun setLine(sourceLine: SourceLine) {
        line = sourceLine
        lineLength = sourceLine.content.length
    }

    private fun checkPosition(lineIndex: Int, index: Int) {
        require(lineIndex in lines.indices) {
            "Line index $lineIndex out of range, number of lines: ${lines.size}"
        }
        val sourceLine = lines[lineIndex]
        require(index in 0..sourceLine.content.length) {
            "Index $index out of range, line length: ${sourceLine.content.length}"
        }
    }

    companion object {
        const val END: Char = '\u0000'

        private val WHITESPACE = charArrayOf(' ', '\t', '\n', '\u000B', '\u000C', '\r')

        @JvmStatic
        fun of(lines: SourceLines): Scanner = Scanner(lines.lines, 0, 0)

        private fun Char.isHighSurrogate(): Boolean = code in 0xD800..0xDBFF

        private fun Char.isLowSurrogate(): Boolean = code in 0xDC00..0xDFFF

        private fun toCodePoint(high: Char, low: Char): Int =
            ((high.code - 0xD800) shl 10) + (low.code - 0xDC00) + 0x10000
    }
}
