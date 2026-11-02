package io.heapy.kotmark.ext.gfm.alerts

import io.heapy.kotmark.Extension
import io.heapy.kotmark.node.Emphasis
import io.heapy.kotmark.node.SourceSpan
import io.heapy.kotmark.node.Text
import io.heapy.kotmark.parser.IncludeSourceSpans
import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.html.htmlRenderer
import io.heapy.kotmark.renderer.markdown.markdownRenderer
import io.heapy.kotmark.renderer.text.LineBreakRendering
import io.heapy.kotmark.renderer.text.textContentRenderer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class AlertsTest {
    private val extensions: List<Extension> = listOf(AlertsExtension.create())
    private val parser =
        parser {
            extensions(extensions)
            includeSourceSpans = IncludeSourceSpans.BLOCKS_AND_INLINES
        }
    private val html = htmlRenderer { extensions(extensions) }

    @Test
    fun configurationSnapshotsAllowedTypesAndValidatesAssignments() {
        lateinit var configuration: AlertsExtension.Configuration
        val types = mutableMapOf("INFO" to "Information")
        val extension = alertsExtension {
            configuration = this
            allowedTypes = types
            types.clear()
        }
        configuration.allowedTypes = mapOf("NOTE" to "Changed")
        val parser = parser { extensions(listOf(extension)) }
        val renderer = htmlRenderer { extensions(listOf(extension)) }
        assertEquals(
            alertHtml("info", "Information", "<p>Body</p>\n"),
            renderer.render(parser.parse("> [!INFO]\n> Body")),
        )
        for (invalid in listOf(mapOf("info" to "Info"), mapOf("" to "Info"), mapOf("INFO" to ""))) {
            assertFailsWith<IllegalArgumentException> { alertsExtension { allowedTypes = invalid } }
        }
    }

    @Test
    fun standardTypesAndContent() {
        val cases =
            mapOf(
                "NOTE" to "Note",
                "TIP" to "Tip",
                "IMPORTANT" to "Important",
                "WARNING" to "Warning",
                "CAUTION" to "Caution",
            )
        for ([type, title] in cases) {
            assertHtml(
                "> [!$type]\n> This is content",
                alertHtml(type.lowercase(), title, "<p>This is content</p>\n"),
            )
        }
        assertHtml(
            """
                |> [!note]
                |> First paragraph
                |>
                |> Second **paragraph**
            """.trimMargin(),
            alertHtml(
                "note",
                "Note",
                """
                    |<p>First paragraph</p>
                    |<p>Second <strong>paragraph</strong></p>
                    |
                """.trimMargin(),
            ),
        )
        assertHtml(
            "> [!NOTE]\nBody text",
            alertHtml("note", "Note", "") + "<p>Body text</p>\n",
        )
    }

    @Test
    fun customTypesAndValidation() {
        val extension =
            alertsExtension {
                addCustomType("INFO", "Information")
                addCustomType("NOTE", "Nota")
            }
        val customParser = parser { extensions(listOf(extension)) }
        val customHtml = htmlRenderer { extensions(listOf(extension)) }
        assertEquals(
            alertHtml("info", "Information", "<p>Custom alert</p>\n"),
            customHtml.render(customParser.parse("> [!INFO]\n> Custom alert")),
        )
        assertEquals(
            alertHtml("note", "Nota", "<p>Localized title</p>\n"),
            customHtml.render(customParser.parse("> [!NOTE]\n> Localized title")),
        )
        assertFailsWith<IllegalArgumentException> {
            alertsExtension { addCustomType("info", "Information") }
        }
        assertFailsWith<IllegalArgumentException> {
            alertsExtension { addCustomType("", "Title") }
        }
        assertFailsWith<IllegalArgumentException> {
            alertsExtension { addCustomType("INFO", "") }
        }
        assertFailsWith<IllegalArgumentException> {
            alertsExtension { allowedTypes = emptyMap() }
        }
    }

    @Test
    fun customTitlesAndSourceSpans() {
        val customExtensions =
            listOf(alertsExtension { customTitlesAllowed = true })
        val customParser =
            parser {
                extensions(customExtensions)
                includeSourceSpans = IncludeSourceSpans.BLOCKS_AND_INLINES
            }
        val customHtml = htmlRenderer { extensions(customExtensions) }
        val source = "> [!NOTE] Custom _title_\n> Body text"
        assertEquals(
            alertHtml("note", "Custom <em>title</em>", "<p>Body text</p>\n"),
            customHtml.render(customParser.parse(source)),
        )
        assertEquals(
            alertHtml("important", """See <a href="https://example.com">docs</a> or <code>run()</code>""", "<p>Body</p>\n"),
            customHtml.render(
                customParser.parse(
                    "> [!IMPORTANT] See [docs](https://example.com) or `run()`\n> Body",
                ),
            ),
        )
        assertEquals(
            alertHtml("note", "Custom <em>title</em>", ""),
            customHtml.render(customParser.parse("> [!NOTE] Custom _title_")),
        )
        assertEquals(
            alertHtml("note", "Note", "<p>Body text</p>\n"),
            customHtml.render(customParser.parse("> [!NOTE]   \n> Body text")),
        )

        val alert = customParser.parse(source).firstChild as Alert
        val title = assertIs<AlertTitle>(alert.firstChild)
        assertEquals(listOf(SourceSpan.of(0, 10, 10, 14)), title.getSourceSpans())
        val text = assertIs<Text>(title.firstChild)
        assertEquals(listOf(SourceSpan.of(0, 10, 10, 7)), text.getSourceSpans())
        val emphasis = assertIs<Emphasis>(text.next)
        assertEquals(listOf(SourceSpan.of(0, 17, 17, 7)), emphasis.getSourceSpans())
    }

    @Test
    fun nestingIsConfigurable() {
        val source = """
            |> [!TIP]
            |> Body
            |> > [!IMPORTANT]
            |> > Nested body
        """.trimMargin()
        assertHtml(
            source,
            alertHtml(
                "tip",
                "Tip",
                """
                    |<p>Body</p>
                    |<blockquote>
                    |<p>[!IMPORTANT]
                    |Nested body</p>
                    |</blockquote>
                    |
                """.trimMargin(),
            ),
        )

        val nestedExtensions =
            listOf(alertsExtension { nestedAlertsAllowed = true })
        val nestedParser = parser { extensions(nestedExtensions) }
        val nestedHtml = htmlRenderer { extensions(nestedExtensions) }
        assertEquals(
            alertHtml(
                "tip",
                "Tip",
                "<p>Body</p>\n" +
                    alertHtml("important", "Important", "<p>Nested body</p>\n"),
            ),
            nestedHtml.render(nestedParser.parse(source)),
        )
    }

    @Test
    fun markdownRoundTrips() {
        val renderer = markdownRenderer { extensions(extensions) }
        for (
            input in
                listOf(
                    """
                        |> [!WARNING]
                        |> Be careful
                        |
                    """.trimMargin(),
                    "> [!NOTE]\n> Note\n",
                    "> [!TIP]\n> Items:\n> \n> - First\n> - Second\n",
                    "> [!NOTE]\n",
                    """
                        |- List
                        |
                        |> [!NOTE]
                        |
                        |- List
                        |
                    """.trimMargin(),
                )
        ) {
            assertEquals(input, renderer.render(parser.parse(input)))
        }
        assertEquals(
            "> [!NOTE]\n> Content\n",
            renderer.render(parser.parse("> [!note]\n> Content\n")),
        )

        val customExtensions =
            listOf(alertsExtension { customTitlesAllowed = true })
        val customParser = parser { extensions(customExtensions) }
        val customRenderer = markdownRenderer { extensions(customExtensions) }
        val titled = """
            |> [!WARNING] Custom _title **with `formatting`**_
            |> Be careful
            |
        """.trimMargin()
        assertEquals(titled, customRenderer.render(customParser.parse(titled)))
    }

    @Test
    fun textRenderingModes() {
        val source = """
            |> [!NOTE]
            |> First paragraph
            |>
            |> Second paragraph
        """.trimMargin()
        assertText(source, LineBreakRendering.COMPACT, "Note\nFirst paragraph\nSecond paragraph")
        assertText(
            source,
            LineBreakRendering.SEPARATE_BLOCKS,
            """
                |Note
                |
                |First paragraph
                |
                |Second paragraph
            """.trimMargin(),
        )
        assertText(source, LineBreakRendering.STRIP, "Note: First paragraph Second paragraph")
    }

    private fun assertText(source: String, mode: LineBreakRendering, expected: String) {
        val renderer =
            textContentRenderer {
                extensions(extensions)
                lineBreakRendering = mode
            }
        assertEquals(expected, renderer.render(parser.parse(source)))
    }

    private fun assertHtml(source: String, expected: String) {
        assertEquals(expected, html.render(parser.parse(source)))
    }

    private fun alertHtml(type: String, title: String, body: String): String =
        """
            |<div class="markdown-alert markdown-alert-$type" data-alert-type="$type">
            |<p class="markdown-alert-title">$title</p>
            |
        """.trimMargin() +
            body +
            "</div>\n"
}
