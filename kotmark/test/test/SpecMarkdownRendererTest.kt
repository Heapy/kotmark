package io.heapy.kotmark.test

import io.heapy.kotmark.testfixtures.TestFixtures
import io.heapy.kotmark.parser.Parser
import io.heapy.kotmark.renderer.html.HtmlRenderer
import io.heapy.kotmark.renderer.markdown.MarkdownRenderer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class SpecMarkdownRendererTest {
    @Test
    fun everyBundledSpecExampleSurvivesMarkdownRoundTrip() {
        val parser = Parser.builder().build()
        val markdownRenderer = MarkdownRenderer.builder().build()
        val htmlRenderer = HtmlRenderer.builder().percentEncodeUrls(true).build()
        val spec = TestFixtures.readText("commonmark-java/spec.txt")
        val failures = mutableListOf<String>()
        var count = 0
        for (example in readExamples(spec)) {
            count++
            val markdown = markdownRenderer.render(parser.parse(example.source))
            val actual = htmlRenderer.render(parser.parse(markdown)).replace('\t', '\u2192')
            if (actual != example.html) {
                failures +=
                    "example $count (${example.section})\n" +
                        "source:\n${example.source}\nmarkdown:\n$markdown\n" +
                        "expected:\n${example.html}\nactual:\n$actual"
            }
        }
        assertEquals(652, count, "Complete CommonMark corpus must execute")
        if (failures.isNotEmpty()) {
            fail(
                "${failures.size}/$count Markdown round-trip examples failed:\n\n" +
                    failures.take(20).joinToString("\n---\n"),
            )
        }
    }

    private fun readExamples(spec: String): List<Example> {
        val examples = mutableListOf<Example>()
        var state = 0
        var section = ""
        var source = StringBuilder()
        var html = StringBuilder()
        for (line in spec.lineSequence()) {
            if (state == 0) {
                SECTION_PATTERN.matchEntire(line)?.let { section = it.groupValues[1] }
            }
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
                        examples += Example(section, source.toString(), html.toString())
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

    private data class Example(
        val section: String,
        val source: String,
        val html: String,
    )

    companion object {
        private val SECTION_PATTERN = Regex("#{1,6} *(.*)")
        private const val EXAMPLE_START = "```````````````````````````````` example"
        private const val EXAMPLE_END = "````````````````````````````````"
    }
}
