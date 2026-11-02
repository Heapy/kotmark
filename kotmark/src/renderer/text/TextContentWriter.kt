package io.heapy.kotmark.renderer.text

class TextContentWriter(
    private val buffer: Appendable,
    private val lineBreakRendering: LineBreakRendering = LineBreakRendering.COMPACT,
) {
    private val prefixes = mutableListOf<String>()
    private val tight = mutableListOf<Boolean>()
    private var blockSeparator: String? = null
    private var lastChar: Char = '\u0000'

    fun whitespace() {
        if (lastChar != '\u0000' && lastChar != ' ') {
            write(' ')
        }
    }

    fun colon() {
        if (lastChar != '\u0000' && lastChar != ':') {
            write(':')
        }
    }

    fun line() {
        append('\n')
        writePrefixes()
    }

    fun block() {
        blockSeparator =
            when {
                lineBreakRendering == LineBreakRendering.STRIP -> " "
                lineBreakRendering == LineBreakRendering.COMPACT || isTight() -> "\n"
                else -> "\n\n"
            }
    }

    fun resetBlock() {
        blockSeparator = null
    }

    fun writeStripped(value: String) {
        write(WHITESPACE.replace(value, " "))
    }

    fun write(value: String) {
        flushBlockSeparator()
        append(value)
    }

    fun write(value: Char) {
        flushBlockSeparator()
        append(value)
    }

    fun pushPrefix(prefix: String) {
        prefixes.add(prefix)
    }

    fun writePrefix(prefix: String) {
        write(prefix)
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

    private fun isTight(): Boolean = tight.lastOrNull() == true

    private fun writePrefixes() {
        for (prefix in prefixes) {
            append(prefix)
        }
    }

    private fun flushBlockSeparator() {
        val separator = blockSeparator ?: return
        if (separator == "\n" || separator == "\n\n") {
            for (character in separator) {
                append(character)
                writePrefixes()
            }
        } else {
            append(separator)
        }
        blockSeparator = null
    }

    private fun append(value: String) {
        buffer.append(value)
        if (value.isNotEmpty()) {
            lastChar = value.last()
        }
    }

    private fun append(value: Char) {
        buffer.append(value)
        lastChar = value
    }

    companion object {
        private val WHITESPACE = Regex("""[\r\n\s]+""")
    }
}
