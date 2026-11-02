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
            "<ul>\n<li>$HTML_CHECKED this is <em>done</em></li>\n</ul>\n",
        )
        assertHtml(
            "- [ ] do this\n",
            "<ul>\n<li>$HTML_UNCHECKED do this</li>\n</ul>\n",
        )
        assertHtml(
            "- [x] foo\n  - [ ] bar\n  - [x] baz\n- [ ] bim",
            "<ul>\n" +
                "<li>$HTML_CHECKED foo\n" +
                "<ul>\n" +
                "<li>$HTML_UNCHECKED bar</li>\n" +
                "<li>$HTML_CHECKED baz</li>\n" +
                "</ul>\n" +
                "</li>\n" +
                "<li>$HTML_UNCHECKED bim</li>\n" +
                "</ul>\n",
        )
        assertHtml(
            "*   [ ]   do this\n*   [ ]   and this",
            "<ul>\n<li>$HTML_UNCHECKED do this</li>\n<li>$HTML_UNCHECKED and this</li>\n</ul>\n",
        )
        assertHtml(
            "+ [x] one\n  - [ ] two\n    * [x] three\n",
            "<ul>\n" +
                "<li>$HTML_CHECKED one\n" +
                "<ul>\n" +
                "<li>$HTML_UNCHECKED two\n" +
                "<ul>\n" +
                "<li>$HTML_CHECKED three</li>\n" +
                "</ul>\n" +
                "</li>\n" +
                "</ul>\n" +
                "</li>\n" +
                "</ul>\n",
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
            "* [  ] nor this\n* [XX] nor this\n",
            "<ul>\n<li>[  ] nor this</li>\n<li>[XX] nor this</li>\n</ul>\n",
        )
        assertHtml(
            "+ text before [x] is not a task\n",
            "<ul>\n<li>text before [x] is not a task</li>\n</ul>\n",
        )
        assertHtml(
            "* [x]  \n* [ ]  \n",
            "<ul>\n<li>[x]</li>\n<li>[ ]</li>\n</ul>\n",
        )
    }

    @Test
    fun markdownRoundTrips() {
        for (
            input in
                listOf(
                    "- [x] I am checked\n",
                    "- [ ] I am unchecked\n",
                    "- [x] I am checked\n- [ ] I am unchecked\n",
                    "- [ ] I am unchecked\n  - [x] I am a checked child\n",
                    "- [x] I am **boldly** checked\n- [ ] I am *italicly* unchecked\n",
                    "- [x] I am checked\n- [ ] I am unchecked\n- I am not a task item\n",
                    "1. [x] I am checked\n2. [ ] I am unchecked\n",
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
        private const val HTML_CHECKED = "<input type=\"checkbox\" disabled=\"\" checked=\"\">"
        private const val HTML_UNCHECKED = "<input type=\"checkbox\" disabled=\"\">"
    }
}
