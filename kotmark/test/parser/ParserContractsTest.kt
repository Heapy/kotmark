package io.heapy.kotmark.parser

import io.heapy.kotmark.internal.BlockContinueImpl
import io.heapy.kotmark.internal.BlockStartImpl
import io.heapy.kotmark.internal.Definitions
import io.heapy.kotmark.internal.InlineParserContextImpl
import io.heapy.kotmark.internal.inline.LinkResultImpl
import io.heapy.kotmark.internal.inline.ParsedInlineImpl
import io.heapy.kotmark.node.Block
import io.heapy.kotmark.node.DefinitionMap
import io.heapy.kotmark.node.Link
import io.heapy.kotmark.node.LinkReferenceDefinition
import io.heapy.kotmark.node.SourceSpan
import io.heapy.kotmark.node.Text
import io.heapy.kotmark.node.Visitor
import io.heapy.kotmark.parser.beta.LinkResult
import io.heapy.kotmark.parser.beta.ParsedInline
import io.heapy.kotmark.parser.beta.Scanner
import io.heapy.kotmark.parser.block.AbstractBlockParser
import io.heapy.kotmark.parser.block.BlockContinue
import io.heapy.kotmark.parser.block.BlockStart
import io.heapy.kotmark.parser.block.ParserState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ParserContractsTest {
    @Test
    fun definitionsKeepFirstNormalizedLabel() {
        val first = LinkReferenceDefinition("Foo  Bar", "/first", null)
        val second = LinkReferenceDefinition("foo bar", "/second", null)
        val map = DefinitionMap(LinkReferenceDefinition::class)
        assertNull(map.putIfAbsent(first.label!!, first))
        assertSame(first, map.putIfAbsent(second.label!!, second))

        val definitions = Definitions()
        definitions.addDefinitions(map)
        assertSame(first, definitions.getDefinition(LinkReferenceDefinition::class, " FOO\nBAR "))

        val context = InlineParserContextImpl(
            emptyList(),
            emptyList(),
            emptyList(),
            emptySet(),
            definitions,
        )
        assertSame(first, context.getDefinition(LinkReferenceDefinition::class, "foo bar"))
    }

    @Test
    fun blockContinueFactories() {
        assertNull(BlockContinue.none())

        val byIndex = assertIs<BlockContinueImpl>(BlockContinue.atIndex(4))
        assertEquals(4, byIndex.newIndex)
        assertEquals(-1, byIndex.newColumn)
        assertFalse(byIndex.isFinalize)

        val byColumn = assertIs<BlockContinueImpl>(BlockContinue.atColumn(8))
        assertEquals(-1, byColumn.newIndex)
        assertEquals(8, byColumn.newColumn)
        assertFalse(byColumn.isFinalize)

        assertTrue(assertIs<BlockContinueImpl>(BlockContinue.finished()).isFinalize)
    }

    @Test
    fun blockStartFactories() {
        val parser = TestBlockParser()
        val start = assertIs<BlockStartImpl>(
            BlockStart.of(parser)
                .atIndex(3)
                .atColumn(7)
                .replaceParagraphLines(2),
        )
        assertEquals(listOf(parser), start.blockParsers.toList())
        assertEquals(3, start.newIndex)
        assertEquals(7, start.newColumn)
        assertEquals(2, start.replaceParagraphLines)
        assertFailsWith<IllegalArgumentException> {
            start.replaceParagraphLines(0)
        }
    }

    @Test
    fun inlineResultFactories() {
        val scanner = Scanner.of(SourceLines.of(SourceLine.of("x", null)))
        val position = scanner.position()
        val text = Text("x")

        assertNull(ParsedInline.none())
        val parsed = assertIs<ParsedInlineImpl>(ParsedInline.of(text, position))
        assertSame(text, parsed.node)
        assertSame(position, parsed.position)

        assertNull(LinkResult.none())
        val link = Link("/url", null)
        val result = assertIs<LinkResultImpl>(LinkResult.wrapTextIn(link, position))
        assertEquals(LinkResultImpl.Type.WRAP, result.type)
        assertFalse(result.isIncludeMarker)
        assertSame(result, result.includeMarker())
        assertTrue(result.isIncludeMarker)
    }

    private class TestBlock : Block() {
        override fun accept(visitor: Visitor) = Unit
    }

    private class TestBlockParser : AbstractBlockParser() {
        override val block: Block = TestBlock()

        override fun tryContinue(parserState: ParserState): BlockContinue? = null

        override fun addSourceSpan(sourceSpan: SourceSpan) {
            super.addSourceSpan(sourceSpan)
        }
    }
}
