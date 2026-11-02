package io.heapy.kotmark.parser.delimiter

interface DelimiterProcessor {
    val openingCharacter: Char
    val closingCharacter: Char
    val minLength: Int

    fun process(openingRun: DelimiterRun, closingRun: DelimiterRun): Int
}
