package org.commonmark.parser

import org.commonmark.node.SourceSpan
import kotlin.jvm.JvmStatic

/**
 * A set of [SourceLine] values from the input source.
 *
 * @since 0.16.0
 */
class SourceLines {
    val lines: MutableList<SourceLine> = mutableListOf()

    fun addLine(sourceLine: SourceLine) {
        lines.add(sourceLine)
    }

    fun isEmpty(): Boolean = lines.isEmpty()

    val content: String
        get() = lines.joinToString(separator = "\n") { it.content }

    val sourceSpans: List<SourceSpan>
        get() = lines.mapNotNull { it.sourceSpan }

    companion object {
        @JvmStatic
        fun empty(): SourceLines = SourceLines()

        @JvmStatic
        fun of(sourceLine: SourceLine): SourceLines =
            SourceLines().apply { addLine(sourceLine) }

        @JvmStatic
        fun of(sourceLines: List<SourceLine>): SourceLines =
            SourceLines().apply { lines.addAll(sourceLines) }
    }
}
