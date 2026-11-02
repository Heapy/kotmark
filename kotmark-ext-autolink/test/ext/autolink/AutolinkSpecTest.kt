package io.heapy.kotmark.ext.autolink

import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.html.htmlRenderer
import kotlin.test.Test
import kotlin.test.assertEquals

class AutolinkSpecTest {
    @Test
    fun matchesUpstreamOnGfmCorpus() {
        val extensions = listOf(AutolinkExtension.create())
        val parser = parser { extensions(extensions) }
        val renderer = htmlRenderer { extensions(extensions) }
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
