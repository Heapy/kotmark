package org.commonmark.parser

import org.commonmark.internal.BlockContinueImpl
import org.commonmark.internal.BlockStartImpl
import org.commonmark.internal.Definitions
import org.commonmark.internal.InlineParserContextImpl
import org.commonmark.internal.inline.LinkResultImpl
import org.commonmark.internal.inline.ParsedInlineImpl
import org.commonmark.node.Block
import org.commonmark.node.DefinitionMap
import org.commonmark.node.Link
import org.commonmark.node.LinkReferenceDefinition
import org.commonmark.node.SourceSpan
import org.commonmark.node.Text
import org.commonmark.node.Visitor
import org.commonmark.parser.beta.LinkResult
import org.commonmark.parser.beta.ParsedInline
import org.commonmark.parser.beta.Scanner
import org.commonmark.parser.block.AbstractBlockParser
import org.commonmark.parser.block.BlockContinue
import org.commonmark.parser.block.BlockStart
import org.commonmark.parser.block.ParserState
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
