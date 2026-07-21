package org.commonmark.internal

internal class BlockContent(
    content: String = "",
) {
    private val builder = StringBuilder(content)
    private var lineCount: Int = 0

    fun add(line: CharSequence) {
        if (lineCount != 0) {
            builder.append('\n')
        }
        builder.append(line)
        lineCount++
    }

    val string: String
        get() = builder.toString()
}
