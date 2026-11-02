package io.heapy.kotmark.integration

import io.heapy.kotmark.parser.Parser
import io.heapy.kotmark.testutil.TestResources
import io.heapy.kotmark.testutil.example.ExampleReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class BoundsIntegrationTest {
    private val parser = Parser.builder().build()

    @Test
    fun everySpecExampleCanBeTruncatedAtEveryBoundary() {
        val sources = ExampleReader.readExampleSources(TestResources.getSpec())
        assertEquals(652, sources.size, "Complete CommonMark corpus must execute at every boundary")
        for (source in sources) {
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
