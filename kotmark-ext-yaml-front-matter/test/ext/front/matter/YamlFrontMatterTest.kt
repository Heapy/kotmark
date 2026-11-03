package io.heapy.kotmark.ext.front.matter

import io.heapy.kotmark.Extension
import io.heapy.kotmark.node.Document
import io.heapy.kotmark.node.Paragraph
import io.heapy.kotmark.node.Text
import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.html.htmlRenderer
import io.heapy.kotmark.renderer.markdown.markdownRenderer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class YamlFrontMatterTest {
    private val extensions: List<Extension> = listOf(YamlFrontMatterExtension.create())
    private val parser = parser { extensions(extensions) }
    private val html = htmlRenderer { extensions(extensions) }
    private val markdown = markdownRenderer { extensions(extensions) }

    @Test
    fun parsesValuesAndOmitsFrontMatterFromHtml() {
        assertFrontMatter(
            """
                |---
                |hello: world
                |...
                |
                |great
            """.trimMargin(),
            mapOf("hello" to listOf("world")),
        )
        assertFrontMatter(
            "---\nkey:\n---\n\ngreat",
            mapOf("key" to emptyList()),
        )
        assertFrontMatter(
            """
                |---
                |list:
                |  - value1
                |  - value2
                |...
                |
                |great
            """.trimMargin(),
            mapOf("list" to listOf("value1", "value2")),
        )
        assertFrontMatter(
            """
                |---
                |literal: |
                |  hello markdown!
                |  literal thing...
                |---
                |
                |great
            """.trimMargin(),
            mapOf("literal" to listOf("hello markdown!\nliteral thing...")),
        )
        assertEquals(
            "<p>great</p>\n",
            html.render(parser.parse("---\nhello: world\n---\n\ngreat")),
        )
    }

    @Test
    fun longLiteralBlockPreservesTrimmedLinesAndBlankLines() {
        val lines = listOf("") + List(5_000) { index ->
            if (index % 17 == 0) "  " else "  line $index  "
        } + listOf("")
        assertFrontMatter(
            "---\nliteral: |\n${lines.joinToString("\n")}\nnext: 'done'\n---\n",
            mapOf("literal" to listOf(lines.joinToString("\n") { it.trim() }), "next" to listOf("done")),
        )
    }

    @Test
    fun valuesStaySeparateAcrossKeyTransitionsAndParserReuse() {
        val document = parser.parse(
            "---\nfirst: |\n  first line\n  second line\n" +
                "quoted: 'it''s quoted'\nlist:\n  - 'one'\n  - \"two\"\n" +
                "empty:\nlast: |\n  final line\n---\n",
        )
        val expected = mapOf(
            "first" to listOf("first line\nsecond line"),
            "quoted" to listOf("it's quoted"),
            "list" to listOf("one", "two"),
            "empty" to emptyList(),
            "last" to listOf("final line"),
        )
        val original = YamlFrontMatterVisitor.readData(document)
        assertEquals(expected, original)
        assertFrontMatter("---\nfirst: |\n  replacement\n---\n", mapOf("first" to listOf("replacement")))
        assertEquals(expected, original)
        assertEquals(expected, YamlFrontMatterVisitor.readData(document))
    }

    @Test
    fun onlyRecognizesFrontMatterAtDocumentStart() {
        val input = """
            |# hello
            |
            |hello markdown world!
            |---
            |hello: world
            |---
        """.trimMargin()
        assertTrue(readData(input).isEmpty())
        assertEquals(
            """
                |<h1>hello</h1>
                |<h2>hello markdown world!</h2>
                |<h2>hello: world</h2>
                |
            """.trimMargin(),
            html.render(parser.parse(input)),
        )
        assertTrue(readData("----\ntest").isEmpty())
        assertTrue(readData("---\n---\ntest").isEmpty())
    }

    @Test
    fun markdownRoundTripsParsedValues() {
        for (
            input in
                listOf(
                    """
                        |---
                        |title: My Document
                        |---
                        |
                        |Markdown content
                        |
                    """.trimMargin(),
                    """
                        |---
                        |key:
                        |---
                        |
                        |Markdown content
                        |
                    """.trimMargin(),
                    """
                        |---
                        |title: My Document
                        |author: John Doe
                        |---
                        |
                        |Markdown content
                        |
                    """.trimMargin(),
                    """
                        |---
                        |tags:
                        |  - java
                        |  - markdown
                        |---
                        |
                        |Markdown content
                        |
                    """.trimMargin(),
                    """
                        |---
                        |description: |
                        |  first line
                        |  second line
                        |---
                        |
                        |Markdown content
                        |
                    """.trimMargin(),
                    """
                        |---
                        |key: 'value with ''single quotes'''
                        |---
                        |
                        |Markdown content
                        |
                    """.trimMargin(),
                    """
                        |---
                        |tags: [java, markdown]
                        |---
                        |
                        |Markdown content
                        |
                    """.trimMargin(),
                    """
                        |---
                        |data: {key: value}
                        |---
                        |
                        |Markdown content
                        |
                    """.trimMargin(),
                    """
                        |---
                        |---
                        |
                        |Markdown content
                        |
                    """.trimMargin(),
                )
        ) {
            assertEquals(input, markdown.render(parser.parse(input)))
        }
        assertEquals(
            """
                |---
                |key: 'value with "double quotes"'
                |---
                |
                |Markdown content
                |
            """.trimMargin(),
            markdown.render(
                parser.parse(
                    """
                        |---
                        |key: "value with \"double quotes\""
                        |---
                        |
                        |Markdown content
                        |
                    """.trimMargin(),
                ),
            ),
        )
    }

    @Test
    fun programmaticValuesAreQuotedWhenNeeded() {
        assertProgrammatic("title", listOf("My Document"), "title: My Document")
        assertProgrammatic("key", listOf("value with a: colon inside"), "key: 'value with a: colon inside'")
        assertProgrammatic("time", listOf("12:30:00"), "time: 12:30:00")
        assertProgrammatic("key", listOf("- not a list"), "key: '- not a list'")
        assertProgrammatic("key", listOf("[broken"), "key: '[broken'")
        assertProgrammatic("flowList", listOf("[1, 2, 3]"), "flowList: [1, 2, 3]")
        assertProgrammatic("flowMapping", listOf("{key: val}"), "flowMapping: {key: val}")
        assertProgrammatic("key", listOf("value # not a comment"), "key: 'value # not a comment'")
        assertProgrammatic("key", listOf("it's a test"), "key: 'it''s a test'")
        assertProgrammatic("empty", listOf(""), "empty: ''")
    }

    private fun assertFrontMatter(source: String, expected: Map<String, List<String>>) {
        assertEquals(expected, readData(source))
    }

    private fun readData(source: String): Map<String, List<String>> {
        val visitor = YamlFrontMatterVisitor()
        parser.parse(source).accept(visitor)
        return visitor.data
    }

    private fun assertProgrammatic(key: String, values: List<String>, expectedLine: String) {
        val document = Document()
        val frontMatter = YamlFrontMatterBlock()
        frontMatter.appendChild(YamlFrontMatterNode(key, values))
        document.appendChild(frontMatter)
        val paragraph = Paragraph()
        paragraph.appendChild(Text("Markdown content"))
        document.appendChild(paragraph)
        assertEquals(
            """
                |---
                |$expectedLine
                |---
                |
                |Markdown content
                |
            """.trimMargin(),
            markdown.render(document),
        )
    }
}
