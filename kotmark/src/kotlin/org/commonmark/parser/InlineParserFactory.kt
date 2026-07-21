package org.commonmark.parser

fun interface InlineParserFactory {
    fun create(inlineParserContext: InlineParserContext): InlineParser
}
