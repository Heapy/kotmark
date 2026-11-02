package io.heapy.kotmark.ext.gfm.alerts

import io.heapy.kotmark.parser.Parser
import io.heapy.kotmark.renderer.html.HtmlRenderer
import io.heapy.kotmark.testfixtures.example.ExampleReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class AlertsSpecTest {
    @Test
    fun upstreamAlertsSpec() {
        val extension = AlertsExtension.create()
        val parser = Parser.builder().extensions(listOf(extension)).build()
        val renderer =
            HtmlRenderer.builder()
                .extensions(listOf(extension))
                .softbreak("<br>\n")
                .build()
        val examples = ExampleReader.readExamples("commonmark-java/alerts-spec.txt")

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
        assertEquals(33, count, "Complete alerts corpus must execute")
        if (failures.isNotEmpty()) {
            fail("${failures.size}/$count alert examples failed:\n\n${failures.take(20).joinToString("\n---\n")}")
        }
    }
}
