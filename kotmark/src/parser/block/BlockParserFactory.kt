package io.heapy.kotmark.parser.block

fun interface BlockParserFactory {
    fun tryStart(
        state: ParserState,
        matchedBlockParser: MatchedBlockParser,
    ): BlockStart?
}
