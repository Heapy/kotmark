package io.heapy.kotmark.publication

import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.kotlinxhtml.kotlinxHtmlRenderer
import kotlinx.html.div
import kotlinx.html.stream.createHTML
import kotlin.test.Test
import kotlin.test.assertEquals

class RendererOnlyTest {
    @Test
    fun standardMarkdownDoesNotNeedAnyExtension() {
        val document = parser().parse("Plain & < text")
        assertEquals(
            "<div><p>Plain &amp; &lt; text</p></div>",
            createHTML(prettyPrint = false).div { kotlinxHtmlRenderer().render(document, this) },
        )
    }
}
