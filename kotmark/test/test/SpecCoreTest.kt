package io.heapy.kotmark.test

import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.html.htmlRenderer
import io.heapy.kotmark.testfixtures.example.ExampleReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class SpecCoreTest {
    @Test
    fun commonMarkJavaBundledSpec() {
        assertSpec("commonmark-java/spec.txt", expectedExampleCount = 652)
    }

    @Test
    fun pinnedCommonMarkReferenceSpec() {
        assertSpec("commonmark-spec/spec.txt", expectedExampleCount = 655)
    }

    @Test
    fun commonMarkJavaBundledSpecWithCrLfInput() {
        assertSpec(
            "commonmark-java/spec.txt",
            expectedExampleCount = 652,
            sourceTransform = { source -> source.replace("\n", "\r\n") },
        )
    }

    @Test
    fun upstreamRegressionFixtures() {
        val overrides =
            mapOf(
                "[XSS](javascript&amp;colon;alert%28&#039;XSS&#039;%29)\n" to
                    """<p><a href="javascript&amp;colon;alert%28'XSS'%29">XSS</a></p>""" + "\n",
                "\uFEFF# Hi\n" to "<p>\uFEFF# Hi</p>\n",
            )
        assertSpec(
            "commonmark-java/cmark-regression.txt",
            expectedExampleCount = 15,
            expectedOverrides = overrides,
        )
        assertSpec(
            "commonmark-java/commonmark.js-regression.txt",
            expectedExampleCount = 20,
            expectedOverrides = overrides,
        )
    }

    private fun assertSpec(
        resource: String,
        expectedExampleCount: Int,
        sourceTransform: (String) -> String = { it },
        expectedOverrides: Map<String, String> = emptyMap(),
    ) {
        val parser = parser()
        val renderer = htmlRenderer { percentEncodeUrls = true }
        val failures = mutableListOf<String>()
        var count = 0
        for (example in ExampleReader.readExamples(resource)) {
            count++
            val actual = renderer.render(parser.parse(sourceTransform(example.source)))
            val comparableActual = actual.replace('\t', '\u2192')
            val expected = expectedOverrides[example.source] ?: example.html
            if (comparableActual != expected) {
                failures +=
                    buildString {
                        append("""section """")
                        append(example.section)
                        append("""" example """)
                        append(example.exampleNumber)
                        append("\nsource:\n")
                        append(example.source)
                        append("expected:\n")
                        append(expected)
                        append("actual:\n")
                        append(comparableActual)
                    }
            }
        }
        assertEquals(expectedExampleCount, count, "Complete corpus must execute: $resource")
        if (failures.isNotEmpty()) {
            fail(
                "${failures.size}/$count CommonMark examples failed:\n\n" +
                    failures.take(20).joinToString("\n\n---\n\n"),
            )
        }
    }
}
