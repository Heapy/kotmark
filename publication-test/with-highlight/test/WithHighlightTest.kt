package io.heapy.kotmark.publication

import io.heapy.kotmark.ext.highlight.HighlightExtension
import io.heapy.kotmark.renderer.kotlinxhtml.adapters.highlightAdapter
import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.kotlinxhtml.kotlinxHtmlRenderer
import kotlinx.html.div
import kotlinx.html.stream.createHTML
import kotlin.test.Test
import kotlin.test.assertEquals

class WithHighlightTest {
    @Test
    fun highlightAdapterWorksWithoutMath() {
        val document = parser { extensions(listOf(HighlightExtension.create())) }.parse("==x & y==")
        val renderer = kotlinxHtmlRenderer { adapters(highlightAdapter()) }
        assertEquals(
            "<div><p><mark>x &amp; y</mark></p></div>",
            createHTML(prettyPrint = false).div { renderer.render(document, this) },
        )
    }
}
