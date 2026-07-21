package org.commonmark.integration

import org.commonmark.Extension
import org.commonmark.ext.autolink.AutolinkExtension
import org.commonmark.ext.footnotes.FootnotesExtension
import org.commonmark.ext.front.matter.YamlFrontMatterExtension
import org.commonmark.ext.gfm.alerts.AlertsExtension
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.ext.image.attributes.ImageAttributesExtension
import org.commonmark.ext.ins.InsExtension
import org.commonmark.ext.task.list.items.TaskListItemsExtension
import org.commonmark.parser.IncludeSourceSpans
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.commonmark.renderer.markdown.MarkdownRenderer
import org.commonmark.testutil.TestResources
import org.commonmark.testutil.example.ExampleReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class IntegrationTest {
    private val extensions: List<Extension> =
        listOf(
            AutolinkExtension.create(),
            FootnotesExtension.create(),
            ImageAttributesExtension.create(),
            InsExtension.create(),
            AlertsExtension.create(),
            StrikethroughExtension.create(),
            TablesExtension.create(),
            TaskListItemsExtension.create(),
            YamlFrontMatterExtension.create(),
        )
    private val parser = Parser.builder().extensions(extensions).build()
    private val html =
        HtmlRenderer.builder()
            .extensions(extensions)
            .percentEncodeUrls(true)
            .build()

    @Test
    fun extensionsWorkTogether() {
        assertEquals(
            "<p><img src=\"/url.png\" alt=\"text\" height=\"5\" width=\"6\" /></p>\n",
            html.render(parser.parse("![text](/url.png){height=5 width=6}")),
        )
        assertEquals(
            "<ul>\n" +
                "<li><input type=\"checkbox\" disabled=\"\"> task to do</li>\n" +
                "<li><input type=\"checkbox\" disabled=\"\" checked=\"\"> task done</li>\n" +
                "</ul>\n",
            html.render(parser.parse("- [ ] task to do\n- [x] task done\n")),
        )
        assertEquals(
            "> [!NOTE]\n> |Column|\n> |---|\n> |~deleted~|\n",
            MarkdownRenderer.builder()
                .extensions(extensions)
                .build()
                .render(
                    parser.parse(
                        "> [!NOTE]\n> |Column|\n> |---|\n> |~deleted~|\n",
                    ),
                ),
        )
    }

    @Test
    fun allExtensionsPreserveTheCoreSpec() {
        assertSpec(Parser.builder().extensions(extensions).build())
    }

    @Test
    fun allExtensionsWithBlockSourceSpansPreserveTheCoreSpec() {
        assertSpec(
            Parser.builder()
                .extensions(extensions)
                .includeSourceSpans(IncludeSourceSpans.BLOCKS)
                .build(),
        )
    }

    private fun assertSpec(integrationParser: Parser) {
        val overrides =
            mapOf(
                "<https://foo.bar/baz bim>\n" to
                    "<p>&lt;<a href=\"https://foo.bar/baz\">https://foo.bar/baz</a> bim&gt;</p>\n",
                "<foo\\+@bar.example.com>\n" to
                    "<p>&lt;<a href=\"mailto:foo+@bar.example.com\">foo+@bar.example.com</a>&gt;</p>\n",
                "< https://foo.bar >\n" to
                    "<p>&lt; <a href=\"https://foo.bar\">https://foo.bar</a> &gt;</p>\n",
                "https://example.com\n" to
                    "<p><a href=\"https://example.com\">https://example.com</a></p>\n",
                "foo@bar.example.com\n" to
                    "<p><a href=\"mailto:foo@bar.example.com\">foo@bar.example.com</a></p>\n",
                "---\nFoo\n---\nBar\n---\nBaz\n" to "<h2>Bar</h2>\n<p>Baz</p>\n",
                "---\n---\n" to "",
            )
        val failures = mutableListOf<String>()
        val examples = ExampleReader.readExamples(TestResources.getSpec())
        for (example in examples) {
            val expected = overrides[example.source] ?: example.html
            val actual = html.render(integrationParser.parse(example.source)).replace('\t', '\u2192')
            if (actual != expected) {
                failures +=
                    "${example}\nsource:\n${example.source}\nexpected:\n$expected\nactual:\n$actual"
            }
        }
        if (failures.isNotEmpty()) {
            fail(
                "${failures.size}/${examples.size} integration examples failed:\n\n" +
                    failures.take(20).joinToString("\n---\n"),
            )
        }
    }
}
