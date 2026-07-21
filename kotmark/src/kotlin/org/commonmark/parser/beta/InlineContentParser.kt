package org.commonmark.parser.beta

/**
 * Parser for one kind of inline content.
 */
fun interface InlineContentParser {
    fun tryParse(inlineParserState: InlineParserState): ParsedInline?
}
