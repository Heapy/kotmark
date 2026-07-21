package org.commonmark.node

import org.commonmark.parser.Parser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame

class NodeBehaviorTest {
    @Test
    fun visitorCanReplaceNodesWithoutDestroyingVisitOrder() {
        val visitor =
            object : AbstractVisitor() {
                override fun visit(text: Text) {
                    text.insertAfter(Code(text.literal))
                    text.unlink()
                }
            }
        val paragraph = Paragraph()
        paragraph.appendChild(Text("foo"))
        paragraph.appendChild(Text("bar"))
        paragraph.accept(visitor)

        val first = assertIs<Code>(paragraph.firstChild)
        val second = assertIs<Code>(first.next)
        assertEquals("foo", first.literal)
        assertEquals("bar", second.literal)
        assertNull(second.next)
        assertSame(second, paragraph.lastChild)
    }

    @Test
    fun parsedEmphasisRetainsDelimiters() {
        val document =
            Parser.builder().build().parse(
                "* *emphasis* \n* **strong** \n* _important_ \n* __CRITICAL__ \n",
            )
        val delimiters = mutableListOf<Delimited>()
        document.accept(
            object : AbstractVisitor() {
                override fun visit(emphasis: Emphasis) {
                    delimiters += emphasis
                }

                override fun visit(strongEmphasis: StrongEmphasis) {
                    delimiters += strongEmphasis
                }
            },
        )
        assertEquals(
            listOf("*" to "*", "**" to "**", "_" to "_", "__" to "__"),
            delimiters.map { it.openingDelimiter to it.closingDelimiter },
        )
    }

    @Test
    fun sourceSpanSubSpans() {
        val span = SourceSpan.of(1, 2, 3, 5)
        assertSame(span, span.subSpan(0))
        assertSame(span, span.subSpan(0, 5))
        assertEquals(SourceSpan.of(1, 3, 4, 4), span.subSpan(1))
        assertEquals(SourceSpan.of(1, 4, 5, 3), span.subSpan(2))
        assertEquals(SourceSpan.of(1, 5, 6, 2), span.subSpan(3))
        assertEquals(SourceSpan.of(1, 6, 7, 1), span.subSpan(4))
        assertEquals(SourceSpan.of(1, 7, 8, 0), span.subSpan(5))
        assertEquals(SourceSpan.of(1, 2, 3, 4), span.subSpan(0, 4))
        assertEquals(SourceSpan.of(1, 2, 3, 0), span.subSpan(0, 0))
        assertEquals(SourceSpan.of(1, 3, 4, 3), span.subSpan(1, 4))
        assertEquals(SourceSpan.of(1, 4, 5, 1), span.subSpan(2, 3))
    }

    @Test
    fun sourceSpanSubSpanValidatesBounds() {
        val span = SourceSpan.of(1, 2, 3, 5)
        assertFailsWith<IndexOutOfBoundsException> { span.subSpan(-1) }
        assertFailsWith<IndexOutOfBoundsException> { span.subSpan(6) }
        assertFailsWith<IndexOutOfBoundsException> { span.subSpan(0, -1) }
        assertFailsWith<IndexOutOfBoundsException> { span.subSpan(0, 6) }
        assertFailsWith<IndexOutOfBoundsException> { span.subSpan(2, 1) }
    }
}
