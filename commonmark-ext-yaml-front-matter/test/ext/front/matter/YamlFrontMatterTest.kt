package io.heapy.kotmark.ext.front.matter

import io.heapy.kotmark.Extension
import io.heapy.kotmark.node.Document
import io.heapy.kotmark.node.Paragraph
import io.heapy.kotmark.node.Text
import io.heapy.kotmark.parser.Parser
import io.heapy.kotmark.renderer.html.HtmlRenderer
import io.heapy.kotmark.renderer.markdown.MarkdownRenderer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class YamlFrontMatterTest {
    private val extensions: List<Extension> = listOf(YamlFrontMatterExtension.create())
    private val parser = Parser.builder().extensions(extensions).build()
    private val html = HtmlRenderer.builder().extensions(extensions).build()
    private val markdown = MarkdownRenderer.builder().extensions(extensions).build()

    @Test
    fun parsesValuesAndOmitsFrontMatterFromHtml() {
        assertFrontMatter(
            "---\nhello: world\n...\n\ngreat",
            mapOf("hello" to listOf("world")),
        )
        assertFrontMatter(
            "---\nkey:\n---\n\ngreat",
            mapOf("key" to emptyList()),
        )
        assertFrontMatter(
            "---\nlist:\n  - value1\n  - value2\n...\n\ngreat",
            mapOf("list" to listOf("value1", "value2")),
        )
        assertFrontMatter(
            "---\nliteral: |\n  hello markdown!\n  literal thing...\n---\n\ngreat",
            mapOf("literal" to listOf("hello markdown!\nliteral thing...")),
        )
        assertEquals(
            "<p>great</p>\n",
            html.render(parser.parse("---\nhello: world\n---\n\ngreat")),
        )
    }

    @Test
    fun onlyRecognizesFrontMatterAtDocumentStart() {
        val input = "# hello\n\nhello markdown world!\n---\nhello: world\n---"
        assertTrue(readData(input).isEmpty())
        assertEquals(
            "<h1>hello</h1>\n<h2>hello markdown world!</h2>\n<h2>hello: world</h2>\n",
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
                    "---\ntitle: My Document\n---\n\nMarkdown content\n",
                    "---\nkey:\n---\n\nMarkdown content\n",
                    "---\ntitle: My Document\nauthor: John Doe\n---\n\nMarkdown content\n",
                    "---\ntags:\n  - java\n  - markdown\n---\n\nMarkdown content\n",
                    "---\ndescription: |\n  first line\n  second line\n---\n\nMarkdown content\n",
                    "---\nkey: 'value with ''single quotes'''\n---\n\nMarkdown content\n",
                    "---\ntags: [java, markdown]\n---\n\nMarkdown content\n",
                    "---\ndata: {key: value}\n---\n\nMarkdown content\n",
                    "---\n---\n\nMarkdown content\n",
                )
        ) {
            assertEquals(input, markdown.render(parser.parse(input)))
        }
        assertEquals(
            "---\nkey: 'value with \"double quotes\"'\n---\n\nMarkdown content\n",
            markdown.render(
                parser.parse(
                    "---\nkey: \"value with \\\"double quotes\\\"\"\n---\n\nMarkdown content\n",
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
            "---\n$expectedLine\n---\n\nMarkdown content\n",
            markdown.render(document),
        )
    }
}
