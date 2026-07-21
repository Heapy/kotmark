package org.commonmark.parser.beta

interface InlineContentParserFactory {
    val triggerCharacters: Set<Char>

    fun create(): InlineContentParser
}
