package io.heapy.kotmark.integration

import io.heapy.kotmark.parser.Parser
import io.heapy.kotmark.testutil.TestResources
import io.heapy.kotmark.testutil.example.ExampleReader
import kotlin.test.Test
import kotlin.test.fail

class BoundsIntegrationTest {
    private val parser = Parser.builder().build()

    @Test
    fun everySpecExampleCanBeTruncatedAtEveryBoundary() {
        for (source in ExampleReader.readExampleSources(TestResources.getSpec())) {
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
