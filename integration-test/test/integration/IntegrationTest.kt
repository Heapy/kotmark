package io.heapy.kotmark.integration

import io.heapy.kotmark.Extension
import io.heapy.kotmark.ext.autolink.AutolinkExtension
import io.heapy.kotmark.ext.footnotes.FootnotesExtension
import io.heapy.kotmark.ext.front.matter.YamlFrontMatterExtension
import io.heapy.kotmark.ext.front.matter.YamlFrontMatterVisitor
import io.heapy.kotmark.ext.front.matter.parser.RawContentParser
import io.heapy.kotmark.ext.gfm.alerts.AlertsExtension
import io.heapy.kotmark.ext.gfm.strikethrough.StrikethroughExtension
import io.heapy.kotmark.ext.gfm.tables.TablesExtension
import io.heapy.kotmark.ext.highlight.HighlightExtension
import io.heapy.kotmark.ext.image.attributes.ImageAttributesExtension
import io.heapy.kotmark.ext.ins.InsExtension
import io.heapy.kotmark.ext.math.MathExtension
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
    @Test
    fun inlineNestingLimitIncludesExtensionDelimitersAndImages() {
        val parser = parser {
            maxInlineNesting = 1
            extensions(listOf(InsExtension.create()))
        }
        val renderer = htmlRenderer { extensions(listOf(InsExtension.create())) }
        assertEquals("<p>++<ins>x</ins>++</p>\n", renderer.render(parser.parse("++++x++++")))
        assertEquals("<p>++<img src=\"u\" alt=\"x\" />++</p>\n", renderer.render(parser.parse("++![x](u)++")))
    }

    private val extensions: List<Extension> =
        listOf(
            AutolinkExtension.create(),
            FootnotesExtension.create(),
            ImageAttributesExtension.create(),
            InsExtension.create(),
            HighlightExtension.create(),
            MathExtension.create(),
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
            "<p><mark><span class=\"math math-inline\">x &lt; y</span> and <ins>new</ins> <del>old</del></mark></p>\n",
            html.render(parser.parse("==\$x < y$ and ++new++ ~~old~~==")),
        )
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
    fun rawFrontMatterDoesNotParseOtherExtensionSyntaxAndPreservesTheBody() {
        val rawExtensions = extensions.filterNot { it is YamlFrontMatterExtension } +
            YamlFrontMatterExtension.create(RawContentParser.Factory())
        val rawParser = parser { extensions(rawExtensions) }
        val rawMarkdown = markdownRenderer { extensions(rawExtensions) }
        val content = "title: '==highlight== and ++insert++'\nlink: https://example.com\n"
        val body = "|Column|\n|---|\n|~~deleted~~|\n\n- [x] task done\n"
        val document = rawParser.parse("---\n${content}---\n\n$body")

        assertEquals(content, YamlFrontMatterVisitor.readRawContent(document))
        assertEquals(html.render(parser.parse(body)), html.render(document))
        val reparsed = rawParser.parse(rawMarkdown.render(document))
        assertEquals(content, YamlFrontMatterVisitor.readRawContent(reparsed))
        assertEquals(html.render(document), html.render(reparsed))
    }

    @Test
    fun mathAndHighlightTruncatedInputs() {
        val source = "==[\$x < y$](/url)== and ++new++\n\n> $$\n> \\frac{a}{b}\n> $$\n"
        val renderer = markdownRenderer { extensions(extensions) }
        for (end in 0..source.length) {
            val document = parser.parse(source.take(end))
            assertEquals(html.render(document), html.render(parser.parse(renderer.render(document))), "prefix $end")
        }
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
