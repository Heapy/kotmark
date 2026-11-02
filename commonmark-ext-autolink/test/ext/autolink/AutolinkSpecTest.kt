package io.heapy.kotmark.ext.autolink

import io.heapy.kotmark.parser.Parser
import io.heapy.kotmark.renderer.html.HtmlRenderer
import kotlin.test.Test
import kotlin.test.assertEquals

class AutolinkSpecTest {
    @Test
    fun matchesUpstreamOnGfmCorpus() {
        val extensions = listOf(AutolinkExtension.create())
        val parser = Parser.builder().extensions(extensions).build()
        val renderer = HtmlRenderer.builder().extensions(extensions).build()
        assertEquals(11, gfmAutolinkExamples.size)
        for (example in gfmAutolinkExamples) {
            assertEquals(example.upstreamHtml, renderer.render(parser.parse(example.source)), "GFM example ${example.number}")
        }
    }

    @Test
    fun gfmDifferencesAreExplicit() {
        // Preserve the original specification expectations alongside reference outputs.
        assertEquals(listOf(5, 6, 11), gfmAutolinkExamples.filter { it.gfmHtml != it.upstreamHtml }.map { it.number })
    }
}
