package io.heapy.kotmark.parser

import java.io.StringReader
import io.heapy.kotmark.renderer.html.HtmlRenderer
import kotlin.test.Test
import kotlin.test.assertEquals

class ParserReaderTest {
    @Test
    fun readerAndStringParsingAreEquivalent() {
        val source = "# Heading\r\n\r\nparagraph *with emphasis*"
        val parser = Parser.builder().build()
        val renderer = HtmlRenderer.builder().build()
        assertEquals(
            renderer.render(parser.parse(source)),
            renderer.render(parser.parseReader(StringReader(source))),
        )
    }
}
