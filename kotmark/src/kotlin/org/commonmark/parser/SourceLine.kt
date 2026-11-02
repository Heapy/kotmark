package org.commonmark.parser

import org.commonmark.node.SourceSpan
import kotlin.jvm.JvmStatic

/**
 * A line or part of a line from the input source.
 *
 * @since 0.16.0
 */
class SourceLine private constructor(
    val content: CharSequence,
    val sourceSpan: SourceSpan?,
) {
    fun substring(beginIndex: Int, endIndex: Int): SourceLine {
        if (beginIndex < 0 || endIndex > content.length || beginIndex > endIndex) {
            throw IndexOutOfBoundsException("beginIndex=$beginIndex, endIndex=$endIndex, length=${content.length}")
        }
        val newContent = content.subSequence(beginIndex, endIndex)
        val newSourceSpan = sourceSpan?.let { span ->
            val length = endIndex - beginIndex
            if (length == 0) {
                null
            } else {
                SourceSpan.of(
                    span.lineIndex,
                    span.columnIndex + beginIndex,
                    span.inputIndex + beginIndex,
                    length,
                )
            }
        }
        return of(newContent, newSourceSpan)
    }

    companion object {
        @JvmStatic
        fun of(content: CharSequence, sourceSpan: SourceSpan?): SourceLine =
            SourceLine(content, sourceSpan)
    }
}
