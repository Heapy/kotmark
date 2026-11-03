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

    /**
     * Moves backward by one UTF-16 code unit, reversing [next].
     * Crossing a line boundary lands on its synthetic newline. There is no newline after the
     * final line: from EOF this moves to the last character, or the preceding newline when the
     * final line is empty. At the start of input, including empty input, this does nothing.
     */
    fun prev() {
        if (index > 0) {
            index--
        } else if (lineIndex > 0) {
            lineIndex--
            // Preserve next()'s representation when stepping back from repeated calls at EOF.
            setLine(if (lineIndex < lines.size) lines[lineIndex] else SourceLine.of("", null))
            index = lineLength
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

    /**
     * Finds [c] at or before the current position, including synthetic newlines but never [END].
     * Returns the UTF-16 units moved backward, or -1 on a miss, leaving the cursor at input start.
     * An immediate match returns 0; matching the last character from EOF returns 1.
     */
    fun findPrev(c: Char): Int = findPrev(c, null, 0, 0)

    /**
     * Like [findPrev] for a character, but uses [matcher]. [END] is never passed to the matcher.
     * Returns the UTF-16 units moved backward, or -1 with the cursor at input start on a miss.
     */
    fun findPrev(matcher: CharMatcher): Int = findPrev(END, matcher, 0, 0)

    /**
     * Finds [c] at or before the cursor and at or after the inclusive [bound], never matching [END].
     * Returns the UTF-16 units moved backward, or -1 with the cursor at [bound] on a miss.
     * The canonical start position is a valid bound for empty input, where the result is -1.
     * Positions should come from this scanner, as with [setPosition].
     * @throws IllegalArgumentException before moving if [bound] is outside this input or after the cursor.
     */
    fun findPrev(c: Char, bound: Position): Int {
        checkLowerBound(bound)
        return findPrev(c, null, bound.lineIndex, bound.index)
    }

    /**
     * Finds a character matched by [matcher] between the cursor and inclusive [bound].
     * [END] is never passed to [matcher]. Returns the UTF-16 units moved backward, or -1 with the
     * cursor at [bound] on a miss. The canonical start position is valid for empty input.
     * Positions should come from this scanner, as with [setPosition].
     * @throws IllegalArgumentException before moving if [bound] is outside this input or after the cursor.
     */
    fun findPrev(matcher: CharMatcher, bound: Position): Int {
        checkLowerBound(bound)
        return findPrev(END, matcher, bound.lineIndex, bound.index)
    }

    private fun findPrev(c: Char, matcher: CharMatcher?, boundLine: Int, boundIndex: Int): Int {
        var count = 0
        while (true) {
            val current = peek()
            if (current != END && (matcher?.matches(current) ?: (current == c))) {
                return count
            }
            if (lineIndex == boundLine && index == boundIndex) {
                return -1
            }
            val lineBefore = lineIndex
            val indexBefore = index
            prev()
            if (lineIndex == lineBefore && index == indexBefore) {
                return -1
            }
            count++
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

    private fun checkLowerBound(position: Position) {
        if (lines.isEmpty()) {
            require(position.lineIndex == 0) {
                "Line index ${position.lineIndex} out of range, number of lines: ${lines.size}"
            }
            require(position.index == 0) {
                "Index ${position.index} out of range, line length: 0"
            }
        } else {
            checkPosition(position.lineIndex, position.index)
        }
        require(position.lineIndex < lineIndex || position.lineIndex == lineIndex && position.index <= index) {
            "Bound line ${position.lineIndex} index ${position.index} is after the current position line $lineIndex index $index"
        }
    }

    companion object {
        /** End-of-input sentinel, never matched by [find] or [findPrev]. */
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
