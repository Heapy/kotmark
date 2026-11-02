package io.heapy.kotmark.testfixtures

import io.heapy.kotmark.testfixtures.example.ExampleReader
import kotlin.test.Test
import kotlin.test.assertEquals

class TestFixturesTest {
    private val exampleCounts = mapOf(
        "commonmark-java/spec.txt" to 652,
        "commonmark-java/gfm-spec.txt" to 672,
        "commonmark-java/cmark-regression.txt" to 15,
        "commonmark-java/commonmark.js-regression.txt" to 20,
        "commonmark-java/alerts-spec.txt" to 33,
        "commonmark-spec/spec.txt" to 655,
    )

    @Test
    fun bundledCorporaRetainEveryExample() {
        for ([path, expected] in exampleCounts) {
            val count = TestFixtures.readText(path).lineSequence()
                .count { it.startsWith("`".repeat(32) + " example") }
            assertEquals(expected, count, path)
            assertEquals(expected, ExampleReader.readExamples(path).size, "Parsed examples: $path")
        }
    }
}
