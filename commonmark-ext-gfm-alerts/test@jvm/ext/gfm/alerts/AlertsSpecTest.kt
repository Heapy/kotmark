package io.heapy.kotmark.ext.gfm.alerts

import io.heapy.kotmark.testfixtures.TestFixtures
import io.heapy.kotmark.parser.Parser
import io.heapy.kotmark.renderer.html.HtmlRenderer
import kotlin.test.Test
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
        val fixture = TestFixtures.readText("commonmark-java/alerts-spec.txt")

        val failures = mutableListOf<String>()
        var count = 0
        for ((source, expected) in readExamples(fixture)) {
            count++
            val actual = renderer.render(parser.parse(source))
            if (actual != expected) {
                failures +=
                    "example $count\nsource:\n$source\nexpected:\n$expected\nactual:\n$actual"
            }
        }
        if (failures.isNotEmpty()) {
            fail("${failures.size}/$count alert examples failed:\n\n${failures.take(20).joinToString("\n---\n")}")
        }
    }

    private fun readExamples(spec: String): List<Pair<String, String>> {
        val examples = mutableListOf<Pair<String, String>>()
        var state = 0
        var source = StringBuilder()
        var html = StringBuilder()
        for (line in spec.lineSequence()) {
            when (state) {
                0 ->
                    if (line.startsWith(EXAMPLE_START)) {
                        state = 1
                    }
                1 ->
                    if (line == ".") {
                        state = 2
                    } else {
                        source.append(line.replace('\u2192', '\t')).append('\n')
                    }
                2 ->
                    if (line == EXAMPLE_END) {
                        examples += source.toString() to html.toString()
                        source = StringBuilder()
                        html = StringBuilder()
                        state = 0
                    } else {
                        html.append(line).append('\n')
                    }
            }
        }
        return examples
    }

    companion object {
        private const val EXAMPLE_START = "```````````````````````````````` example"
        private const val EXAMPLE_END = "````````````````````````````````"
    }
}
