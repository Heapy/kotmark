package io.heapy.kotmark.integration

import io.heapy.kotmark.Extension
import io.heapy.kotmark.ext.autolink.AutolinkExtension
import io.heapy.kotmark.ext.footnotes.FootnotesExtension
import io.heapy.kotmark.ext.front.matter.YamlFrontMatterExtension
import io.heapy.kotmark.ext.gfm.alerts.AlertsExtension
import io.heapy.kotmark.ext.gfm.strikethrough.StrikethroughExtension
import io.heapy.kotmark.ext.gfm.tables.TablesExtension
import io.heapy.kotmark.ext.image.attributes.ImageAttributesExtension
import io.heapy.kotmark.ext.ins.InsExtension
import io.heapy.kotmark.ext.task.list.items.TaskListItemsExtension
import io.heapy.kotmark.parser.IncludeSourceSpans
import io.heapy.kotmark.parser.Parser
import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.html.htmlRenderer
import io.heapy.kotmark.renderer.markdown.markdownRenderer
import io.heapy.kotmark.testfixtures.example.ExampleReader
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
    private val parser =
        parser {
            extensions(extensions)
        }
    private val html =
        htmlRenderer {
            extensions(extensions)
            percentEncodeUrls = true
        }

    @Test
    fun extensionsWorkTogether() {
        assertEquals(
            """<p><img src="/url.png" alt="text" height="5" width="6" /></p>""" + "\n",
            html.render(parser.parse("![text](/url.png){height=5 width=6}")),
        )
        assertEquals(
            """
                |<ul>
                |<li><input type="checkbox" disabled=""> task to do</li>
                |<li><input type="checkbox" disabled="" checked=""> task done</li>
                |</ul>
                |
            """.trimMargin(),
            html.render(parser.parse("- [ ] task to do\n- [x] task done\n")),
        )
        assertEquals(
            """
                |> [!NOTE]
                |> |Column|
                |> |---|
                |> |~deleted~|
                |
            """.trimMargin(),
            markdownRenderer { extensions(extensions) }
                .render(
                    parser.parse(
                        """
                            |> [!NOTE]
                            |> |Column|
                            |> |---|
                            |> |~deleted~|
                            |
                        """.trimMargin(),
                    ),
                ),
        )
    }

    @Test
    fun allExtensionsPreserveTheCoreSpec() {
        assertSpec(parser { extensions(extensions) })
    }

    @Test
    fun allExtensionsWithBlockSourceSpansPreserveTheCoreSpec() {
        assertSpec(
            parser {
                extensions(extensions)
                includeSourceSpans = IncludeSourceSpans.BLOCKS
            },
        )
    }

    private fun assertSpec(integrationParser: Parser) {
        val overrides =
            mapOf(
                "<https://foo.bar/baz bim>\n" to
                        """<p>&lt;<a href="https://foo.bar/baz">https://foo.bar/baz</a> bim&gt;</p>""" + "\n",
                """<foo\+@bar.example.com>""" + "\n" to
                        """<p>&lt;<a href="mailto:foo+@bar.example.com">foo+@bar.example.com</a>&gt;</p>""" + "\n",
                "< https://foo.bar >\n" to
                        """<p>&lt; <a href="https://foo.bar">https://foo.bar</a> &gt;</p>""" + "\n",
                "https://example.com\n" to
                        """<p><a href="https://example.com">https://example.com</a></p>""" + "\n",
                "foo@bar.example.com\n" to
                        """<p><a href="mailto:foo@bar.example.com">foo@bar.example.com</a></p>""" + "\n",
                """
                    |---
                    |Foo
                    |---
                    |Bar
                    |---
                    |Baz
                    |
                """.trimMargin() to "<h2>Bar</h2>\n<p>Baz</p>\n",
                "---\n---\n" to "",
            )
        val failures = mutableListOf<String>()
        val examples = ExampleReader.readExamples("commonmark-java/spec.txt")
        assertEquals(652, examples.size, "Complete CommonMark corpus must execute with extensions")
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
