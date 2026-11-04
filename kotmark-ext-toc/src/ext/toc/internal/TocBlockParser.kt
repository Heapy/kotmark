package io.heapy.kotmark.ext.toc.internal

import io.heapy.kotmark.ext.toc.TableOfContents
import io.heapy.kotmark.parser.block.AbstractBlockParser
import io.heapy.kotmark.parser.block.AbstractBlockParserFactory
import io.heapy.kotmark.parser.block.BlockContinue
import io.heapy.kotmark.parser.block.BlockStart
import io.heapy.kotmark.parser.block.MatchedBlockParser
import io.heapy.kotmark.parser.block.ParserState

internal class TocBlockParser(marker: String) : AbstractBlockParser() {
    override val block = TableOfContents(marker)

    override fun tryContinue(parserState: ParserState): BlockContinue? = null

    class Factory(private val markers: Set<String>) : AbstractBlockParserFactory() {
        override fun tryStart(state: ParserState, matchedBlockParser: MatchedBlockParser): BlockStart? {
            if (state.indent >= 4) return null
            val marker = state.line.content.substring(state.nextNonSpaceIndex).trimEnd(' ', '\t')
            if (marker !in markers) return null
            return BlockStart.of(TocBlockParser(marker)).atIndex(state.line.content.length)
        }
    }
}
