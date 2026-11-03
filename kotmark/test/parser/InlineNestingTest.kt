package io.heapy.kotmark.parser

import io.heapy.kotmark.internal.InlineParserImpl
import io.heapy.kotmark.node.Node
import io.heapy.kotmark.node.StrongEmphasis
import io.heapy.kotmark.node.Text
import io.heapy.kotmark.renderer.html.htmlRenderer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class InlineNestingTest {
    private val renderer = htmlRenderer()

    @Test
    fun defaultAndInvalidLimits() {
        val _ = parser { assertEquals(100, maxInlineNesting) }
        assertFailsWith<IllegalArgumentException> { parser { maxInlineNesting = -1 } }
    }

    @Test
    fun emphasisLimitLeavesUnusedDelimitersAsText() {
        val parser = parser { maxInlineNesting = 3 }
        val paragraph = requireNotNull(parser.parse("*".repeat(20) + "x" + "*".repeat(20)).firstChild)
        assertEquals("*".repeat(14), assertIs<Text>(paragraph.firstChild).literal)
        assertEquals("*".repeat(14), assertIs<Text>(paragraph.lastChild).literal)
        var node = paragraph.firstChild?.next
        repeat(3) { node = assertIs<StrongEmphasis>(node).firstChild }
        assertEquals("x", assertIs<Text>(node).literal)
    }

    @Test
    fun zeroLeavesDelimitersLinksAndImagesLiteral() {
        val parser = parser { maxInlineNesting = 0 }
        for (source in listOf("*a*", "[a](u)", "![a](u)")) {
            assertEquals("<p>$source</p>\n", renderer.render(parser.parse(source)))
        }
    }

    @Test
    fun imageLimitAndBracketEmphasisOneLevelCaveat() {
        val parser = parser { maxInlineNesting = 1 }
        assertEquals("<p>![<img src=\"u\" alt=\"a\" />](v)</p>\n", renderer.render(parser.parse("![![a](u)](v)")))
        assertEquals("<p><a href=\"u\"><em>a</em></a></p>\n", renderer.render(parser.parse("[*a*](u)")))
        assertEquals("<p>*<a href=\"u\">a</a>*</p>\n", renderer.render(parser.parse("*[a](u)*")))
    }

    @Test
    fun limitsAreSnapshottedPassedToContextsAndResetForEachFragment() {
        lateinit var configuration: Parser.Configuration
        val contextLimits = mutableListOf<Int>()
        val parser = parser {
            configuration = this
            maxInlineNesting = 1
            inlineParserFactory = InlineParserFactory { context ->
                contextLimits.add(context.maxInlineNesting)
                InlineParserImpl(context)
            }
        }
        configuration.maxInlineNesting = 0
        val one = "<p>*<strong>x</strong>*</p>\n"
        assertEquals(one + one, renderer.render(parser.parse("***x***\n\n***x***")))
        assertEquals(one, renderer.render(parser.parse("***x***")))
        assertEquals(listOf(1, 1, 1), contextLimits)
    }

    @Test
    fun unlimitedAllowsDeeperMixedStructures() {
        val parser = parser { maxInlineNesting = Int.MAX_VALUE }
        val document = parser.parse("*![".repeat(120) + "x" + "](u)*".repeat(120))
        var deepest = 0
        val pending = ArrayDeque<Pair<Node, Int>>()
        pending.addLast(document to 0)
        while (pending.isNotEmpty()) {
            val [node, depth] = pending.removeLast()
            deepest = maxOf(deepest, depth)
            var child = node.firstChild
            while (child != null) {
                pending.addLast(child to depth + 1)
                child = child.next
            }
        }
        assertTrue(deepest > 100)
    }
}
