package io.heapy.kotmark.test

import io.heapy.kotmark.node.Emphasis
import io.heapy.kotmark.node.CustomNode
import io.heapy.kotmark.node.Nodes
import io.heapy.kotmark.node.Text
import io.heapy.kotmark.parser.delimiter.DelimiterProcessor
import io.heapy.kotmark.parser.delimiter.DelimiterRun
import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.html.htmlRenderer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CustomDelimiterProcessorTest {
    @Test
    fun rejectedCloserDoesNotHideAnOpenerFromALaterMatchingLength() {
        val parser = parser { customDelimiterProcessor(EqualLengthProcessor('%', 1)) }
        assertEquals("<p><em>a% b</em></p>\n", htmlRenderer().render(parser.parse("%%%%a% b%%%%")))
    }

    @Test
    fun staggeredCustomProcessorCanMatchAfterRejectingACloser() {
        val parser = parser { customDelimiterProcessor(EqualLengthProcessor('*', 2)) }
        assertEquals("<p><em>a* b</em></p>\n", htmlRenderer().render(parser.parse("****a* b****")))
    }

    @Test
    fun nestingDepthUsesIdentityForCustomNodesWithMutableHashes() {
        val parser = parser {
            maxInlineNesting = 2
            customDelimiterProcessor(IdentitySensitiveProcessor())
        }
        val paragraph = requireNotNull(parser.parse("*%%a%% %b%*").firstChild)
        assertEquals("*", assertIs<Text>(paragraph.firstChild).literal)
        assertEquals("*", assertIs<Text>(paragraph.lastChild).literal)
        val first = assertIs<EqualNode>(paragraph.firstChild?.next)
        assertIs<EqualNode>(first.firstChild)
        assertIs<EqualNode>(first.next?.next)
    }

    private class EqualNode : CustomNode() {
        var hash = 0
        override fun equals(other: Any?): Boolean = other is EqualNode
        override fun hashCode(): Int = hash
    }

    private class IdentitySensitiveProcessor : DelimiterProcessor {
        override val openingCharacter: Char = '%'
        override val closingCharacter: Char = '%'
        override val minLength: Int = 1
        private val created = mutableListOf<EqualNode>()

        override fun process(openingRun: DelimiterRun, closingRun: DelimiterRun): Int {
            created.forEach { it.hash++ }
            val wrapper = EqualNode()
            for (node in Nodes.between(openingRun.opener, closingRun.closer)) {
                wrapper.appendChild(node)
            }
            openingRun.opener.insertAfter(wrapper)
            created.add(wrapper)
            return 1
        }
    }

    private class EqualLengthProcessor(
        override val openingCharacter: Char,
        override val minLength: Int,
    ) : DelimiterProcessor {
        override val closingCharacter: Char = openingCharacter

        override fun process(openingRun: DelimiterRun, closingRun: DelimiterRun): Int {
            if (openingRun.length() != closingRun.length()) return 0
            val emphasis = Emphasis(openingCharacter.toString())
            for (node in Nodes.between(openingRun.opener, closingRun.closer)) {
                emphasis.appendChild(node)
            }
            openingRun.opener.insertAfter(emphasis)
            return openingRun.length()
        }
    }
}
