package io.heapy.kotmark.ext.gfm.tables

import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.html.htmlRenderer
import io.heapy.kotmark.testfixtures.example.ExampleReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class TablesSpecTest {
    @Test
    fun upstreamGfmTableExamples() {
        val extension = TablesExtension.create()
        val parser = parser { extensions(listOf(extension)) }
        val renderer = htmlRenderer { extensions(listOf(extension)) }
        val examples = ExampleReader.readExamples("commonmark-java/gfm-spec.txt")
            .filter { it.section == "Tables (extension)" }
        val failures = mutableListOf<String>()
        var count = 0
        for (example in examples) {
            val source = example.source
            val expected = example.html
            count++
            val actual = renderer.render(parser.parse(source))
            if (actual != expected) {
                failures +=
                    """
                        |example $count
                        |source:
                        |$source
                        |expected:
                        |$expected
                        |actual:
                        |$actual
                    """.trimMargin()
            }
        }
        assertEquals(8, count, "All GFM table examples must execute")
        if (failures.isNotEmpty()) {
            fail("${failures.size}/$count GFM table examples failed:\n\n${failures.joinToString("\n---\n")}")
        }
    }
}
