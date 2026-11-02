package io.heapy.kotmark.testutil

import io.heapy.kotmark.testutil.example.Example
import io.heapy.kotmark.testutil.example.ExampleReader

abstract class SpecTestCase {
    protected lateinit var example: Example

    companion object {
        @JvmStatic
        fun data(): List<Example> = ExampleReader.readExamples(TestResources.getSpec())
    }
}
