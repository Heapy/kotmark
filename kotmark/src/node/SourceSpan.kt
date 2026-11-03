package io.heapy.kotmark.node

import kotlin.jvm.JvmStatic

/**
 * A source span references a snippet of text from the source input.
 *
 * It has a starting position (line and column index) and a length of how many characters it spans.
 */
class SourceSpan private constructor(
    val lineIndex: Int,
    val columnIndex: Int,
    val inputIndex: Int,
    val length: Int,
) {
    init {
        require(lineIndex >= 0) { "lineIndex $lineIndex must be >= 0" }
        require(columnIndex >= 0) { "columnIndex $columnIndex must be >= 0" }
        require(inputIndex >= 0) { "inputIndex $inputIndex must be >= 0" }
        require(length >= 0) { "length $length must be >= 0" }
    }

    fun subSpan(beginIndex: Int): SourceSpan = subSpan(beginIndex, length)

    fun subSpan(beginIndex: Int, endIndex: Int): SourceSpan {
        if (beginIndex < 0) {
            throw IndexOutOfBoundsException("beginIndex $beginIndex must be >= 0")
        }
        if (beginIndex > length) {
            throw IndexOutOfBoundsException("beginIndex $beginIndex must be <= length $length")
        }
        if (endIndex < 0) {
            throw IndexOutOfBoundsException("endIndex $endIndex must be >= 0")
        }
        if (endIndex > length) {
            throw IndexOutOfBoundsException("endIndex $endIndex must be <= length $length")
        }
        if (beginIndex > endIndex) {
            throw IndexOutOfBoundsException("beginIndex $beginIndex must be <= endIndex $endIndex")
        }
        if (beginIndex == 0 && endIndex == length) {
            return this
        }
        return SourceSpan(lineIndex, columnIndex + beginIndex, inputIndex + beginIndex, endIndex - beginIndex)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SourceSpan) return false
        return lineIndex == other.lineIndex &&
            columnIndex == other.columnIndex &&
            inputIndex == other.inputIndex &&
            length == other.length
    }

    override fun hashCode(): Int {
        var result = lineIndex
        result = 31 * result + columnIndex
        result = 31 * result + inputIndex
        result = 31 * result + length
        return result
    }

    override fun toString(): String =
        "SourceSpan{line=$lineIndex, column=$columnIndex, input=$inputIndex, length=$length}"

    companion object {
        @JvmStatic
        fun of(line: Int, col: Int, input: Int, length: Int): SourceSpan =
            SourceSpan(line, col, input, length)
    }
}
