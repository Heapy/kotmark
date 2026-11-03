package io.heapy.kotmark.ext.front.matter

import io.heapy.kotmark.parser.SourceLine

/** Parses front matter lines and appends child nodes to store the result in [YamlFrontMatterBlock]. */
interface FrontMatterParser {
    fun onNextLine(block: YamlFrontMatterBlock, line: SourceLine)

    /**
     * Called for an unindented `---` or `...` separator, not at end of input.
     * Return [SeparatorRole.BLOCK_END] to close the block, or [SeparatorRole.CONTENT] to continue.
     * When treating the separator as content, this callback must process it: [onNextLine] will not
     * be called for the same line. This allows custom parsers to retain separators inside strings.
     */
    fun onEndingSeparator(block: YamlFrontMatterBlock, separator: SourceLine): SeparatorRole

    /** Creates a fresh parser for each front matter block; the factory itself is shared. */
    fun interface Factory {
        fun create(): FrontMatterParser
    }

    enum class SeparatorRole {
        BLOCK_END,
        CONTENT,
    }
}
