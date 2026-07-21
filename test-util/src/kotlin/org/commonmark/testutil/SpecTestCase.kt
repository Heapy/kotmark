package org.commonmark.testutil

import org.commonmark.testutil.example.Example
import org.commonmark.testutil.example.ExampleReader

abstract class SpecTestCase {
    protected lateinit var example: Example

    companion object {
        @JvmStatic
        fun data(): List<Example> = ExampleReader.readExamples(TestResources.getSpec())
    }
}
