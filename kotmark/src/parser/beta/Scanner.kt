package io.heapy.kotmark.parser.beta

import io.heapy.kotmark.node.SourceSpan
import io.heapy.kotmark.parser.SourceLine
import io.heapy.kotmark.parser.SourceLines
import io.heapy.kotmark.text.CharMatcher
import kotlin.jvm.JvmStatic

/**
 * A cursor over source lines for parsing inline content.
 * Lines form one character sequence with a synthetic `\n` between lines, but none after the last.
 * Positions, movement, and returned character counts use UTF-16 code units.
 */
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

    /** Returns the current character without moving, a newline between lines, or [END] at EOF. */
    fun peek(): Char =
        if (index < lineLength) {
            line.content[index]
        } else if (lineIndex < lines.lastIndex) {
            '\n'
        } else {
            END
        }

    /**
     * Returns the current code point without moving. A high surrogate followed by a low surrogate
     * on the same line is combined. Returns a newline between lines or [END]'s code at EOF.
     */
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

    /**
     * Returns the preceding code point without moving, combining a low surrogate with a preceding
     * high surrogate on the same line. At a line's start returns a newline, or [END]'s code at input start.
     */
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

    /** Whether the current position has a character, including a newline between lines. */
    fun hasNext(): Boolean =
        index < lineLength || lineIndex < lines.lastIndex

    /** Advances one UTF-16 code unit, moving to the next line after its preceding newline. */
    fun next() {
        index++
        if (index > lineLength) {
            lineIndex++
            setLine(if (lineIndex < lines.size) lines[lineIndex] else SourceLine.of("", null))
            index = 0
        }
    }

    // Reverses next() by one UTF-16 unit, crossing onto the preceding line's synthetic newline.
    // At input start, including empty input, this does nothing.
    private fun prev() {
        if (index > 0) {
            index--
        } else if (lineIndex > 0) {
            lineIndex--
            // Preserve next()'s representation when stepping back from repeated calls at EOF.
            setLine(if (lineIndex < lines.size) lines[lineIndex] else SourceLine.of("", null))
            index = lineLength
        }
    }

    /** Advances one character if the current character is [c]; otherwise returns false without moving. */
    fun next(c: Char): Boolean {
        if (peek() != c) {
            return false
        }
        next()
        return true
    }

    /**
     * Advances over [content] if it matches entirely within the current line.
     * Returns false without moving on a mismatch or when positioned at the end of the line.
     */
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

    /** Consumes consecutive occurrences of [c], including newlines, returning the UTF-16 units consumed. */
    @IgnorableReturnValue
    fun matchMultiple(c: Char): Int {
        var count = 0
        while (peek() == c) {
            count++
            next()
        }
        return count
    }

    /**
     * Consumes consecutive characters matched by [matcher], returning the UTF-16 units consumed.
     * The matcher receives newlines between lines and [END] at the end of input.
     */
    @IgnorableReturnValue
    fun match(matcher: CharMatcher): Int {
        var count = 0
        while (matcher.matches(peek())) {
            count++
            next()
        }
        return count
    }

    /** Consumes spaces, tabs, newlines, vertical tabs, form feeds, and carriage returns, returning their count. */
    @IgnorableReturnValue
    fun whitespace(): Int {
        var count = 0
        while (peek() in WHITESPACE) {
            count++
            next()
        }
        return count
    }

    /**
     * Finds [c] at or after the current position, including newlines but never [END].
     * Returns the UTF-16 units moved forward and leaves the cursor at the match, or returns -1
     * at EOF on a miss. An immediate match returns 0.
     */
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

    /**
     * Finds a character matched by [matcher] at or after the current position.
     * Returns the UTF-16 units moved forward, leaving the cursor at the match, or -1 at EOF
     * on a miss. [END] is never passed to [matcher].
     */
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
     * Finds [c] at or before the cursor and at or after the inclusive [bound], never matching [END].
     * The current character is included: an immediate match returns 0, and matching the last
     * character from EOF returns 1. Synthetic newlines between lines can be matched.
     * Returns the UTF-16 units moved backward, or -1 with the cursor at [bound] on a miss.
     * A bound equal to the cursor checks only that position; the search never goes before it.
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

    /** Returns the current position for [setPosition], [getSource], or bounded [findPrev]. */
    fun position(): Position = Position(lineIndex, index)

    /**
     * Moves to a position saved from this scanner within its source lines.
     * @throws IllegalArgumentException if the coordinates are outside the source lines.
     */
    fun setPosition(position: Position) {
        checkPosition(position.lineIndex, position.index)
        lineIndex = position.lineIndex
        index = position.index
        setLine(lines[lineIndex])
    }

    /**
     * Returns source from inclusive [begin] to exclusive [end] without moving the cursor.
     * Both positions must be within this scanner's source lines, with [end] not before [begin].
     * Newlines are represented by separate lines, retaining source spans where available.
     */
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
        /**
         * Sentinel for EOF or the beginning when looking backward. CommonMark replaces NUL input
         * before inline scanning. [find] and [findPrev] never match this sentinel.
         */
        const val END: Char = '\u0000'

        private val WHITESPACE = charArrayOf(' ', '\t', '\n', '\u000B', '\u000C', '\r')

        /** Creates a scanner at the beginning of [lines]. */
        @JvmStatic
        fun of(lines: SourceLines): Scanner = Scanner(lines.lines, 0, 0)

        private fun Char.isHighSurrogate(): Boolean = code in 0xD800..0xDBFF

        private fun Char.isLowSurrogate(): Boolean = code in 0xDC00..0xDFFF

        private fun toCodePoint(high: Char, low: Char): Int =
            ((high.code - 0xD800) shl 10) + (low.code - 0xDC00) + 0x10000
    }
}
