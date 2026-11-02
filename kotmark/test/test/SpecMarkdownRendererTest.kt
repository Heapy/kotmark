package io.heapy.kotmark.test

import io.heapy.kotmark.parser.Parser
import io.heapy.kotmark.renderer.html.HtmlRenderer
import io.heapy.kotmark.renderer.markdown.MarkdownRenderer
import io.heapy.kotmark.testfixtures.example.ExampleReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class SpecMarkdownRendererTest {
    @Test
    fun everyBundledSpecExampleSurvivesMarkdownRoundTrip() {
        val parser = Parser.builder().build()
        val markdownRenderer = MarkdownRenderer.builder().build()
        val htmlRenderer = HtmlRenderer.builder().percentEncodeUrls(true).build()
        val examples = ExampleReader.readExamples("commonmark-java/spec.txt")
        val failures = mutableListOf<String>()
        var count = 0
        for (example in examples) {
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
}
