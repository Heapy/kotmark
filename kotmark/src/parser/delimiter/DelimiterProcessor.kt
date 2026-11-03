package io.heapy.kotmark.parser.delimiter

interface DelimiterProcessor {
    val openingCharacter: Char
    val closingCharacter: Char
    val minLength: Int

    /**
     * Creates a wrapper and returns the number of delimiter characters consumed.
     * Insert the wrapper immediately after [DelimiterRun.opener] so the parser can account for
     * its depth when applying [io.heapy.kotmark.parser.Parser.Configuration.maxInlineNesting].
     * Nodes inserted elsewhere still work, but do not participate in that depth accounting.
     * The caller removes the consumed delimiter text nodes.
     */
    fun process(openingRun: DelimiterRun, closingRun: DelimiterRun): Int
}
