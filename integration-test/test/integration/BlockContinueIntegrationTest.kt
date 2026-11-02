package io.heapy.kotmark.integration

import io.heapy.kotmark.node.Block
import io.heapy.kotmark.node.BlockQuote
import io.heapy.kotmark.node.FencedCodeBlock
import io.heapy.kotmark.node.Heading
import io.heapy.kotmark.node.Text
import io.heapy.kotmark.parser.IncludeSourceSpans
import io.heapy.kotmark.parser.Parser
import io.heapy.kotmark.parser.SourceLine
import io.heapy.kotmark.parser.block.AbstractBlockParser
import io.heapy.kotmark.parser.block.BlockContinue
import io.heapy.kotmark.parser.block.BlockStart
import io.heapy.kotmark.parser.block.ParserState
import io.heapy.kotmark.parser.parser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/** Exercises the public extension contract from a separate module. */
class BlockContinueIntegrationTest {
    @Test
    fun indexAndColumnContinuationsHandleTabsDifferently() {
        val cases = listOf(
            BlockContinue.AtIndex(0) to "\tvalue",
            BlockContinue.AtIndex(1) to "value",
            BlockContinue.AtColumn(0) to "\tvalue",
            BlockContinue.AtColumn(1) to "   value",
            BlockContinue.AtColumn(4) to "value",
        )
        for ([outcome, expected] in cases) {
            val blockParser = ExtensionBlockParser { outcome }
            val document = parserFor(blockParser).parse("%%%\n\tvalue")
            assertEquals(listOf("", expected), blockParser.lines)
            assertEquals(1, blockParser.closeCount)
            assertNull(document.firstChild?.next)
        }
    }

    @Test
    fun finishedConsumesClosingLineAndIncludesItsSourceSpan() {
        val blockParser = ExtensionBlockParser {
            if (it.line.content.toString() == "END") BlockContinue.Finished else BlockContinue.AtIndex(0)
        }
        val document = parserFor(blockParser).parse("%%%\nbody\nEND\n# after")
        assertEquals(listOf("", "body"), blockParser.lines)
        assertEquals(1, blockParser.closeCount)
        assertEquals(listOf(0, 1, 2), blockParser.block.getSourceSpans().map { it.lineIndex })
        val heading = assertIs<Heading>(document.firstChild?.next)
        assertEquals("after", assertIs<Text>(heading.firstChild).literal)
        assertNull(heading.next)
    }

    @Test
    fun noContinuationLeavesCurrentLineForAnotherBlock() {
        val blockParser = ExtensionBlockParser { null }
        val document = parserFor(blockParser).parse("%%%\n# after")
        assertEquals(listOf(""), blockParser.lines)
        assertEquals(1, blockParser.closeCount)
        assertEquals(listOf(0), blockParser.block.getSourceSpans().map { it.lineIndex })
        assertIs<Heading>(document.firstChild?.next)
    }

    @Test
    fun finishingContainerClosesItsOpenChildren() {
        val blockParser = ExtensionBlockParser(container = true) {
            if (it.line.content.toString() == "END") BlockContinue.Finished else BlockContinue.AtIndex(0)
        }
        val document = parserFor(blockParser).parse("%%%\n```\nbody\nEND\n# after")
        val quote = assertIs<BlockQuote>(document.firstChild)
        val code = assertIs<FencedCodeBlock>(quote.firstChild)
        assertEquals("body\n", code.literal)
        assertEquals(listOf(0, 1, 2, 3), quote.getSourceSpans().map { it.lineIndex })
        assertEquals(listOf(1, 2, 3), code.getSourceSpans().map { it.lineIndex })
        assertEquals(1, blockParser.closeCount)
        assertIs<Heading>(quote.next)
    }

    private fun parserFor(blockParser: ExtensionBlockParser): Parser = parser {
        includeSourceSpans = IncludeSourceSpans.BLOCKS
        extensions(listOf(Parser.ParserExtension { configuration ->
            configuration.customBlockParserFactory { state, _ ->
                if (state.line.content.toString() == "%%%") BlockStart.of(blockParser).atIndex(3) else null
            }
        }))
    }

    private class ExtensionBlockParser(
        private val container: Boolean = false,
        private val continuation: (ParserState) -> BlockContinue?,
    ) : AbstractBlockParser() {
        override val block: Block = if (container) BlockQuote() else FencedCodeBlock()
        val lines = mutableListOf<String>()
        var closeCount = 0
            private set

        override fun isContainer(): Boolean = container
        override fun canContain(childBlock: Block): Boolean = container
        override fun tryContinue(parserState: ParserState): BlockContinue? = continuation(parserState)
        override fun addLine(line: SourceLine) { lines.add(line.content.toString()) }
        override fun closeBlock() { closeCount++ }
    }
}
