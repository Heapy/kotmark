package io.heapy.kotmark.integration

import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.testfixtures.example.ExampleReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class BoundsIntegrationTest {
    private val parser = parser()

    @Test
    fun everySpecExampleCanBeTruncatedAtEveryBoundary() {
        val examples = ExampleReader.readExamples("commonmark-java/spec.txt")
        assertEquals(652, examples.size, "Complete CommonMark corpus must execute at every boundary")
        for (example in examples) {
            val source = example.source
            for (index in 1 until source.length - 1) {
                parse(source.substring(index))
            }
            for (index in source.length - 1 downTo 2) {
                parse(source.substring(0, index))
            }
        }
    }

    private fun parse(input: String) {
        try {
            parser.parse(input)
        } catch (exception: Exception) {
            fail("Parsing failed for truncated input:\n$input", exception)
        }
    }
}
