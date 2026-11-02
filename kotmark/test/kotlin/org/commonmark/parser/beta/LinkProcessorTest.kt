package org.commonmark.parser.beta

import org.commonmark.node.Link
import org.commonmark.node.Text
import org.commonmark.parser.Parser
import org.commonmark.test.Nodes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class LinkProcessorTest {
    @Test
    fun testLinkMarkerShouldNotBeIncludedByDefault() {
        // If a link marker is registered but is not processed, the built-in link processor shouldn't consume it.
        // And I think by default, other processors shouldn't consume it either (by accident).
        // So requiring processors to opt into including the marker is better than requiring them to opt out,
        // because processors that look for a marker already need to write some code to deal with the marker anyway,
        // and will have tests ensuring that the marker is part of the parsed node, not the text.
        val parser = Parser.builder().linkMarker('^').build()
        val doc = parser.parse("^[test](url)")
        val link = Nodes.find<Link>(doc)
        assertEquals("url", link.destination)
        assertEquals("^", assertIs<Text>(link.previous).literal)
    }
}
