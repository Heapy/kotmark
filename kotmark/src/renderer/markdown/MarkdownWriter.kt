package io.heapy.kotmark.renderer.markdown

import io.heapy.kotmark.text.CharMatcher

class MarkdownWriter(
    private val buffer: Appendable,
    private val lineSeparator: String = "\n",
) {
    private var blockSeparator = 0
    var lastChar: Char = '\u0000'
        private set
    var isAtLineStart: Boolean = true
        private set

    private val prefixes = mutableListOf<String>()
    private val tight = mutableListOf<Boolean>()
    private val rawEscapes = mutableListOf<CharMatcher>()

    fun raw(value: String) {
        flushBlockSeparator()
        write(value, null)
    }

    fun raw(value: Char) {
        flushBlockSeparator()
        write(value)
    }

    fun text(
        value: String,
        escape: CharMatcher,
    ) {
        if (value.isEmpty()) {
            return
        }
        flushBlockSeparator()
        write(value, escape)
        lastChar = value.last()
        isAtLineStart = false
    }

    fun line() {
        write(lineSeparator)
        writePrefixes()
        isAtLineStart = true
    }

    fun block() {
        blockSeparator = if (isTight()) 1 else 2
        isAtLineStart = true
    }

    fun pushPrefix(prefix: String) {
        prefixes.add(prefix)
    }

    fun writePrefix(prefix: String) {
        val wasAtLineStart = isAtLineStart
        raw(prefix)
        isAtLineStart = wasAtLineStart
    }

    fun popPrefix() {
        prefixes.removeAt(prefixes.lastIndex)
    }

    fun pushTight(tight: Boolean) {
        this.tight.add(tight)
    }

    fun popTight() {
        tight.removeAt(tight.lastIndex)
    }

    fun pushRawEscape(rawEscape: CharMatcher) {
        rawEscapes.add(rawEscape)
    }

    fun popRawEscape() {
        rawEscapes.removeAt(rawEscapes.lastIndex)
    }

    private fun write(
        value: String,
        escape: CharMatcher?,
    ) {
        if (rawEscapes.isEmpty() && escape == null) {
            buffer.append(value)
        } else {
            // Append unescaped spans in bulk; Writer implementations may synchronize each call.
            val end = value.length
            var start = 0
            for (index in 0..<end) {
                val character = value[index]
                if (needsEscaping(character, escape)) {
                    if (start < index) {
                        buffer.append(value, start, index)
                    }
                    appendEscaped(character)
                    start = index + 1
                }
            }
            if (start < end) {
                buffer.append(value, start, end)
            }
        }
        if (value.isNotEmpty()) {
            lastChar = value.last()
        }
        isAtLineStart = false
    }

    private fun write(value: String) {
        buffer.append(value)
        // Kotlin/JS string indexing does not reliably throw for an empty string.
        if (value.isEmpty()) throw IndexOutOfBoundsException("Empty line separator")
        lastChar = value[value.lastIndex]
        isAtLineStart = false
    }

    private fun write(character: Char) {
        append(character, null)
        lastChar = character
        isAtLineStart = false
    }

    private fun writePrefixes() {
        for (prefix in prefixes) {
            write(prefix, null)
        }
    }

    private fun flushBlockSeparator() {
        if (blockSeparator != 0) {
            write(lineSeparator)
            writePrefixes()
            if (blockSeparator > 1) {
                write(lineSeparator)
                writePrefixes()
            }
            blockSeparator = 0
        }
    }

    private fun append(
        character: Char,
        escape: CharMatcher?,
    ) {
        if (needsEscaping(character, escape)) {
            appendEscaped(character)
        } else {
            buffer.append(character)
        }
    }

    private fun appendEscaped(character: Char) {
        if (character == '\n') {
            buffer.append("&#10;")
        } else {
            buffer.append('\\')
            buffer.append(character)
        }
    }

    private fun isTight(): Boolean = tight.lastOrNull() == true

    private fun needsEscaping(
        character: Char,
        escape: CharMatcher?,
    ): Boolean =
        escape?.matches(character) == true || rawEscapes.any { it.matches(character) }
}
