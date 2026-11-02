package io.heapy.kotmark.ext.task.list.items

import io.heapy.kotmark.Extension
import io.heapy.kotmark.node.BulletList
import io.heapy.kotmark.node.Document
import io.heapy.kotmark.node.ListItem
import io.heapy.kotmark.node.Paragraph
import io.heapy.kotmark.node.Text
import io.heapy.kotmark.parser.Parser
import io.heapy.kotmark.renderer.html.HtmlRenderer
import io.heapy.kotmark.renderer.markdown.MarkdownRenderer
import kotlin.test.Test
import kotlin.test.assertEquals

class TaskListItemsTest {
    private val extensions: List<Extension> = listOf(TaskListItemsExtension.create())
    private val parser = Parser.builder().extensions(extensions).build()
    private val html = HtmlRenderer.builder().extensions(extensions).build()
    private val markdown = MarkdownRenderer.builder().extensions(extensions).build()

    @Test
    fun syntaxAndHtmlRendering() {
        assertHtml(
            "- [x] this is *done*\n",
            """
                |<ul>
                |<li>$HTML_CHECKED this is <em>done</em></li>
                |</ul>
                |
            """.trimMargin(),
        )
        assertHtml(
            "- [ ] do this\n",
            """
                |<ul>
                |<li>$HTML_UNCHECKED do this</li>
                |</ul>
                |
            """.trimMargin(),
        )
        assertHtml(
            """
                |- [x] foo
                |  - [ ] bar
                |  - [x] baz
                |- [ ] bim
            """.trimMargin(),
            """
                |<ul>
                |<li>$HTML_CHECKED foo
                |<ul>
                |<li>$HTML_UNCHECKED bar</li>
                |<li>$HTML_CHECKED baz</li>
                |</ul>
                |</li>
                |<li>$HTML_UNCHECKED bim</li>
                |</ul>
                |
            """.trimMargin(),
        )
        assertHtml(
            "*   [ ]   do this\n*   [ ]   and this",
            """
                |<ul>
                |<li>$HTML_UNCHECKED do this</li>
                |<li>$HTML_UNCHECKED and this</li>
                |</ul>
                |
            """.trimMargin(),
        )
        assertHtml(
            """
                |+ [x] one
                |  - [ ] two
                |    * [x] three
                |
            """.trimMargin(),
            """
                |<ul>
                |<li>$HTML_CHECKED one
                |<ul>
                |<li>$HTML_UNCHECKED two
                |<ul>
                |<li>$HTML_CHECKED three</li>
                |</ul>
                |</li>
                |</ul>
                |</li>
                |</ul>
                |
            """.trimMargin(),
        )
    }

    @Test
    fun invalidMarkersRemainText() {
        assertHtml("[x] this is not a task\n", "<p>[x] this is not a task</p>\n")
        assertHtml(" [ ] this is not a task either\n", "<p>[ ] this is not a task either</p>\n")
        for (source in listOf("- [x]no space\n", "- [O] is not a _task_\n", "* [] neither is this\n")) {
            val expectedText =
                when {
                    source.startsWith("- [x]") -> "[x]no space"
                    source.startsWith("- [O]") -> "[O] is not a <em>task</em>"
                    else -> "[] neither is this"
                }
            assertHtml(source, "<ul>\n<li>$expectedText</li>\n</ul>\n")
        }
        assertHtml(
            """
                |* [  ] nor this
                |* [XX] nor this
                |
            """.trimMargin(),
            """
                |<ul>
                |<li>[  ] nor this</li>
                |<li>[XX] nor this</li>
                |</ul>
                |
            """.trimMargin(),
        )
        assertHtml(
            "+ text before [x] is not a task\n",
            """
                |<ul>
                |<li>text before [x] is not a task</li>
                |</ul>
                |
            """.trimMargin(),
        )
        assertHtml(
            "* [x]  \n* [ ]  \n",
            """
                |<ul>
                |<li>[x]</li>
                |<li>[ ]</li>
                |</ul>
                |
            """.trimMargin(),
        )
    }

    @Test
    fun markdownRoundTrips() {
        for (
            input in
                listOf(
                    "- [x] I am checked\n",
                    "- [ ] I am unchecked\n",
                    """
                        |- [x] I am checked
                        |- [ ] I am unchecked
                        |
                    """.trimMargin(),
                    """
                        |- [ ] I am unchecked
                        |  - [x] I am a checked child
                        |
                    """.trimMargin(),
                    """
                        |- [x] I am **boldly** checked
                        |- [ ] I am *italicly* unchecked
                        |
                    """.trimMargin(),
                    """
                        |- [x] I am checked
                        |- [ ] I am unchecked
                        |- I am not a task item
                        |
                    """.trimMargin(),
                    """
                        |1. [x] I am checked
                        |2. [ ] I am unchecked
                        |
                    """.trimMargin(),
                )
        ) {
            assertEquals(input, markdown.render(parser.parse(input)))
        }
    }

    @Test
    fun programmaticallyBuiltTaskItem() {
        val document = Document()
        val list = BulletList()
        val item = ListItem()
        val paragraph = Paragraph()
        paragraph.appendChild(Text("I am a task"))
        item.appendChild(TaskListItemMarker(false))
        item.appendChild(paragraph)
        list.appendChild(item)
        document.appendChild(list)

        assertEquals("- [ ] I am a task\n", markdown.render(document))
    }

    private fun assertHtml(source: String, expected: String) {
        assertEquals(expected, html.render(parser.parse(source)))
    }

    companion object {
        private const val HTML_CHECKED = """<input type="checkbox" disabled="" checked="">"""
        private const val HTML_UNCHECKED = """<input type="checkbox" disabled="">"""
    }
}
