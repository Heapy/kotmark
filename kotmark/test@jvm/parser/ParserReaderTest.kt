package io.heapy.kotmark.parser

import io.heapy.kotmark.renderer.html.htmlRenderer
import java.io.StringReader
import kotlin.test.Test
import kotlin.test.assertEquals

class ParserReaderTest {
    @Test
    fun readerAndStringParsingAreEquivalent() {
        val source = "# Heading\r\n\r\nparagraph *with emphasis*"
        val parser = parser()
        val renderer = htmlRenderer()
        assertEquals(
            renderer.render(parser.parse(source)),
            renderer.render(parser.parseReader(StringReader(source))),
        )
    }
}
