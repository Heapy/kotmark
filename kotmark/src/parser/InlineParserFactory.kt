package io.heapy.kotmark.parser

fun interface InlineParserFactory {
    fun create(inlineParserContext: InlineParserContext): InlineParser
}
