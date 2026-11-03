package io.heapy.kotmark.renderer.markdown

import io.heapy.kotmark.text.CharMatcher

class MarkdownWriter(
    private val buffer: Appendable,
    private val lineSeparator: String,
) {
    constructor(buffer: Appendable) : this(buffer, "\n")

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
            for (character in value) {
                append(character, escape)
            }
        }
        if (value.isNotEmpty()) {
            lastChar = value.last()
        }
        isAtLineStart = false
    }

    private fun write(value: String) {
        buffer.append(value)
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
            if (character == '\n') {
                buffer.append("&#10;")
            } else {
                buffer.append('\\')
                buffer.append(character)
            }
        } else {
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
