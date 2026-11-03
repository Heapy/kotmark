package io.heapy.kotmark.test

import io.heapy.kotmark.node.BlockQuote
import io.heapy.kotmark.node.HtmlBlock
import io.heapy.kotmark.node.Paragraph
import io.heapy.kotmark.parser.parser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class HtmlBlockParserTest {
    private val parser = parser()

    @Test
    fun completeTagsPreserveAttributeGrammar() {
        val tags = listOf(
            "<x>",
            "<X-TAG DISABLED>",
            "<x _a:b.c-d9 = 'one > two' q=\"\" raw=value>",
            "<x a=b/>",
            "<x a=/ >",
            "<x a=b/c/>",
            "<x disabled a = \"value\" \t/> \t",
            "</X-tag \t> \t",
        )
        for (tag in tags) {
            val document = parser.parse(tag)
            val block = assertIs<HtmlBlock>(document.firstChild, tag)
            assertEquals(tag, block.literal, tag)
            assertNull(block.next, tag)
        }
    }

    @Test
    fun incompleteTagsAndTrailingContentDoNotStartHtmlBlocks() {
        val tags = listOf(
            "<x a=>",
            "<x a='unterminated>",
            "<x a=\"unterminated>",
            "<x a='b'c>",
            "<x / >",
            "<x a==b>",
            "<x a=`value`>",
            "<x a=b<c>",
            "<x> trailing",
            "</x attr>",
            "</x/>",
        )
        for (tag in tags) {
            assertIs<Paragraph>(parser.parse(tag).firstChild, tag)
        }
    }

    @Test
    fun completeTagsDoNotInterruptParagraphsOrLazyContinuation() {
        val paragraph = assertIs<Paragraph>(parser.parse("text\n<x a=b>").firstChild)
        assertNull(paragraph.next)
        val quote = assertIs<BlockQuote>(parser.parse("> text\n<x a=b>").firstChild)
        assertIs<Paragraph>(quote.firstChild)
        assertNull(quote.next)
    }

    @Test
    fun completeTagWithManyAttributes() {
        val source = "<a" + " a".repeat(4_000) + ">"
        assertEquals(source, assertIs<HtmlBlock>(parser.parse(source).firstChild).literal)
    }
}
