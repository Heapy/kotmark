package org.commonmark.parser.block

fun interface BlockParserFactory {
    fun tryStart(
        state: ParserState,
        matchedBlockParser: MatchedBlockParser,
    ): BlockStart?
}
