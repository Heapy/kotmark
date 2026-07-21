package org.commonmark.test

import java.io.File
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import kotlin.test.Test
import kotlin.test.fail

class SpecCoreTest {
    @Test
    fun commonMarkJavaBundledSpec() {
        assertSpec(
            sequenceOf(
                File("projects/commonmark-java/commonmark-test-util/src/main/resources/spec.txt"),
                File("../projects/commonmark-java/commonmark-test-util/src/main/resources/spec.txt"),
            ).firstOrNull(File::isFile)
                ?: error("Unable to locate the commonmark-java spec fixture from ${File(".").absolutePath}"),
        )
    }

    @Test
    fun currentCommonMarkReferenceSpec() {
        assertSpec(
            sequenceOf(
                File("projects/commonmark-spec/spec.txt"),
                File("../projects/commonmark-spec/spec.txt"),
            ).firstOrNull(File::isFile)
                ?: error("Unable to locate the CommonMark reference spec from ${File(".").absolutePath}"),
        )
    }

    @Test
    fun commonMarkJavaBundledSpecWithCrLfInput() {
        val spec = locate(
            "projects/commonmark-java/commonmark-test-util/src/main/resources/spec.txt",
        )
        assertSpec(
            spec,
            sourceTransform = { source -> source.replace("\n", "\r\n") },
        )
    }

    @Test
    fun upstreamRegressionFixtures() {
        val overrides =
            mapOf(
                "[XSS](javascript&amp;colon;alert%28&#039;XSS&#039;%29)\n" to
                    "<p><a href=\"javascript&amp;colon;alert%28'XSS'%29\">XSS</a></p>\n",
                "\uFEFF# Hi\n" to "<p>\uFEFF# Hi</p>\n",
            )
        assertSpec(
            locate(
                "projects/commonmark-java/commonmark-test-util/src/main/resources/cmark-regression.txt",
            ),
            expectedOverrides = overrides,
        )
        assertSpec(
            locate(
                "projects/commonmark-java/commonmark-test-util/src/main/resources/commonmark.js-regression.txt",
            ),
            expectedOverrides = overrides,
        )
    }

    private fun assertSpec(
        spec: File,
        sourceTransform: (String) -> String = { it },
        expectedOverrides: Map<String, String> = emptyMap(),
    ) {
        val parser = Parser.builder().build()
        val renderer = HtmlRenderer.builder().percentEncodeUrls(true).build()
        val failures = mutableListOf<String>()
        var count = 0
        for (example in readExamples(spec.readText())) {
            count++
            val actual = renderer.render(parser.parse(sourceTransform(example.source)))
            val comparableActual = actual.replace('\t', '\u2192')
            val expected = expectedOverrides[example.source] ?: example.html
            if (comparableActual != expected) {
                failures +=
                    buildString {
                        append("section \"")
                        append(example.section)
                        append("\" example ")
                        append(example.number)
                        append("\nsource:\n")
                        append(example.source)
                        append("expected:\n")
                        append(expected)
                        append("actual:\n")
                        append(comparableActual)
                    }
            }
        }
        if (failures.isNotEmpty()) {
            fail(
                "${failures.size}/$count CommonMark examples failed:\n\n" +
                    failures.take(20).joinToString("\n\n---\n\n"),
            )
        }
    }

    private fun locate(path: String): File =
        sequenceOf(File(path), File("../$path"))
            .firstOrNull(File::isFile)
            ?: error("Unable to locate $path from ${File(".").absolutePath}")

    private fun readExamples(spec: String): List<Example> {
        val examples = mutableListOf<Example>()
        var state = State.BEFORE
        var section = ""
        var exampleNumber = 0
        var source = StringBuilder()
        var html = StringBuilder()

        for (line in spec.lineSequence()) {
            when (state) {
                State.BEFORE -> {
                    SECTION_PATTERN.matchEntire(line)?.let {
                        section = it.groupValues[1]
                        exampleNumber = 0
                    }
                    if (line.startsWith(EXAMPLE_START_MARKER)) {
                        state = State.SOURCE
                        exampleNumber++
                    }
                }
                State.SOURCE -> {
                    if (line == ".") {
                        state = State.HTML
                    } else {
                        source.append(line.replace('\u2192', '\t')).append('\n')
                    }
                }
                State.HTML -> {
                    if (line == EXAMPLE_END_MARKER) {
                        examples += Example(section, exampleNumber, source.toString(), html.toString())
                        source = StringBuilder()
                        html = StringBuilder()
                        state = State.BEFORE
                    } else {
                        html.append(line).append('\n')
                    }
                }
            }
        }
        return examples
    }

    private data class Example(
        val section: String,
        val number: Int,
        val source: String,
        val html: String,
    )

    private enum class State {
        BEFORE,
        SOURCE,
        HTML,
    }

    companion object {
        private val SECTION_PATTERN = Regex("#{1,6} *(.*)")
        private const val EXAMPLE_START_MARKER =
            "```````````````````````````````` example"
        private const val EXAMPLE_END_MARKER =
            "````````````````````````````````"
    }
}
