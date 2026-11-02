package io.heapy.kotmark.parser.beta

import io.heapy.kotmark.node.Link
import io.heapy.kotmark.node.Text
import io.heapy.kotmark.parser.Parser
import io.heapy.kotmark.test.Nodes
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
