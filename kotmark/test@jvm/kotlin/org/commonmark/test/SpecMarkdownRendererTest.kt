package org.commonmark.test

import java.io.File
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.commonmark.renderer.markdown.MarkdownRenderer
import kotlin.test.Test
import kotlin.test.fail

class SpecMarkdownRendererTest {
    @Test
    fun everyBundledSpecExampleSurvivesMarkdownRoundTrip() {
        val parser = Parser.builder().build()
        val markdownRenderer = MarkdownRenderer.builder().build()
        val htmlRenderer = HtmlRenderer.builder().percentEncodeUrls(true).build()
        val spec =
            sequenceOf(
                File("projects/commonmark-java/commonmark-test-util/src/main/resources/spec.txt"),
                File("../projects/commonmark-java/commonmark-test-util/src/main/resources/spec.txt"),
            ).firstOrNull(File::isFile)
                ?: error("Unable to locate spec.txt from ${File(".").absolutePath}")
        val failures = mutableListOf<String>()
        var count = 0
        for (example in readExamples(spec.readText())) {
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
