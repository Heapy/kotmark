package io.heapy.kotmark.parser.beta

interface InlineContentParserFactory {
    val triggerCharacters: Set<Char>

    fun create(): InlineContentParser
}
