package io.heapy.kotmark.parser.beta

import io.heapy.kotmark.node.Link
import io.heapy.kotmark.node.Text
import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.test.Nodes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame

class LinkProcessorTest {
    @Test
    fun linkTextIsAvailableDuringProcessing() {
        val texts = mutableListOf<String>()
        val parser = parser {
            linkProcessor { info, scanner, _ ->
                val position = scanner.position()
                val text = info.text(scanner)
                texts.add(text)
                assertSame(text, info.text(scanner))
                assertEquals(position, scanner.position())
                LinkResult.none()
            }
        }
        parser.parse("![outer\n![inner](image)](outer)\n\n[other](url)")
        parser.parse("[another](url) [](empty)")

        assertEquals(listOf("inner", "outer\n![inner](image)", "other", "another", ""), texts)
    }

    @Test
    fun textExtractionPreservesMovedCursorAndProcessorReset() {
        var initialPosition: Position? = null
        var nextProcessorCalled = false
        val parser = parser {
            linkProcessor { info, scanner, _ ->
                initialPosition = scanner.position()
                scanner.next()
                val movedPosition = scanner.position()
                assertEquals("text", info.text(scanner))
                assertEquals(movedPosition, scanner.position())
                LinkResult.none()
            }
            linkProcessor { info, scanner, _ ->
                nextProcessorCalled = true
                assertEquals(initialPosition, scanner.position())
                assertEquals("text", info.text(scanner))
                LinkResult.none()
            }
        }
        val document = parser.parse("[text](url) tail")
        assertEquals(true, nextProcessorCalled)
        assertEquals("url", Nodes.find<Link>(document).destination)
    }

    @Test
    fun testLinkMarkerShouldNotBeIncludedByDefault() {
        // If a link marker is registered but is not processed, the built-in link processor shouldn't consume it.
        // And I think by default, other processors shouldn't consume it either (by accident).
        // So requiring processors to opt into including the marker is better than requiring them to opt out,
        // because processors that look for a marker already need to write some code to deal with the marker anyway,
        // and will have tests ensuring that the marker is part of the parsed node, not the text.
        val parser = parser { linkMarker('^') }
        val doc = parser.parse("^[test](url)")
        val link = Nodes.find<Link>(doc)
        assertEquals("url", link.destination)
        assertEquals("^", assertIs<Text>(link.previous).literal)
    }
}
