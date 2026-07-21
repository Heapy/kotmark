package org.commonmark.internal

import org.commonmark.node.Block
import org.commonmark.node.Document
import org.commonmark.node.Paragraph
import org.commonmark.parser.SourceLine
import org.commonmark.parser.SourceLines
import org.commonmark.parser.block.BlockParser
import org.commonmark.parser.block.MatchedBlockParser
import org.commonmark.parser.block.ParserState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BlockParsersTest {
    @Test
    fun thematicBreaks() {
        assertTrue(startsThematicBreak("---", 0, 0))
        assertTrue(startsThematicBreak(" * * * ", 1, 1))
        assertFalse(startsThematicBreak("--", 0, 0))
        assertFalse(startsThematicBreak("-_-", 0, 0))
    }

    @Test
    fun blockQuoteFactory() {
        val state = State("> quote", indent = 0, nextNonSpaceIndex = 0)
        val start = BlockQuoteParser.Factory().tryStart(state, Matched(state.activeBlockParser))
        assertNotNull(start)
    }

    @Test
    fun fencedCodeFactoryAndContent() {
        val state = State("``` kotlin", indent = 0, nextNonSpaceIndex = 0)
        val start = FencedCodeBlockParser.Factory().tryStart(state, Matched(state.activeBlockParser))
        assertNotNull(start)
        val parser = org.commonmark.internal.BlockStartImpl::class
        assertNotNull(parser)
    }

    @Test
    fun indentedCodeTrimsTrailingBlankLines() {
        val parser = IndentedCodeBlockParser()
        parser.addLine(SourceLine.of("code", null))
        parser.addLine(SourceLine.of("", null))
        parser.closeBlock()
        assertEquals("code\n", parser.block.literal)
    }

    @Test
    fun documentParserIsContainer() {
        val parser = DocumentBlockParser()
        assertTrue(parser.isContainer())
        assertTrue(parser.canContain(Paragraph()))
        assertIs<Document>(parser.block)
    }

    private fun startsThematicBreak(input: String, indent: Int, nextNonSpace: Int): Boolean {
        val state = State(input, indent, nextNonSpace)
        return ThematicBreakParser.Factory()
            .tryStart(state, Matched(state.activeBlockParser)) != null
    }

    private class State(
        content: String,
        override val indent: Int,
        override val nextNonSpaceIndex: Int,
    ) : ParserState {
        override val line: SourceLine = SourceLine.of(content, null)
        override val index: Int = 0
        override val column: Int = 0
        override val isBlank: Boolean = content.isBlank()
        override val activeBlockParser: BlockParser = DocumentBlockParser()
    }

    private class Matched(
        override val matchedBlockParser: BlockParser,
    ) : MatchedBlockParser {
        override val paragraphLines: SourceLines = SourceLines.empty()
    }
}
