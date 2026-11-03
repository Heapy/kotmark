package io.heapy.kotmark.test

import io.heapy.kotmark.node.Emphasis
import io.heapy.kotmark.node.Nodes
import io.heapy.kotmark.parser.delimiter.DelimiterProcessor
import io.heapy.kotmark.parser.delimiter.DelimiterRun
import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.html.htmlRenderer
import kotlin.test.Test
import kotlin.test.assertEquals

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
