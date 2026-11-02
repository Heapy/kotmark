package io.heapy.kotmark.testfixtures.example

import kotlin.test.Test
import kotlin.test.assertEquals

class ExampleReaderTest {
    @Test
    fun preservesExampleContentAndTracksSectionMetadata() {
        val fence = "`".repeat(32)
        val input = """
            |# First section
            |Explanatory text outside examples is ignored.
            |$fence example option
            |→# Heading inside Markdown
            |
            |.
            |<p>→Tab stays visible in expected HTML</p>
            |# Heading inside HTML
            |$fence
            |$fence example
            |second
            |.
            |<p>second</p>
            |$fence
            |## Next section
            |$fence example another option
            |third
            |.
            |<p>third</p>
            |$fence
        """.trimMargin()
        val expected = listOf(
            Example(
                "fixture.txt", "First section", "option", 1,
                "\t# Heading inside Markdown\n\n",
                "<p>→Tab stays visible in expected HTML</p>\n# Heading inside HTML\n",
            ),
            Example("fixture.txt", "First section", "", 2, "second\n", "<p>second</p>\n"),
            Example("fixture.txt", "Next section", "another option", 1, "third\n", "<p>third</p>\n"),
        )

        for (text in listOf(input, input.replace("\n", "\r\n"))) {
            assertEquals(expected, ExampleReader.parseExamples(text, "fixture.txt"))
        }
    }
}
