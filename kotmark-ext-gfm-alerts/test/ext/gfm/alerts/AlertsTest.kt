package io.heapy.kotmark.ext.gfm.alerts

import io.heapy.kotmark.Extension
import io.heapy.kotmark.node.Emphasis
import io.heapy.kotmark.node.SourceSpan
import io.heapy.kotmark.node.Text
import io.heapy.kotmark.parser.IncludeSourceSpans
import io.heapy.kotmark.parser.Parser
import io.heapy.kotmark.renderer.html.HtmlRenderer
import io.heapy.kotmark.renderer.markdown.MarkdownRenderer
import io.heapy.kotmark.renderer.text.LineBreakRendering
import io.heapy.kotmark.renderer.text.TextContentRenderer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class AlertsTest {
    private val extensions: List<Extension> = listOf(AlertsExtension.create())
    private val parser =
        Parser.builder()
            .extensions(extensions)
            .includeSourceSpans(IncludeSourceSpans.BLOCKS_AND_INLINES)
            .build()
    private val html = HtmlRenderer.builder().extensions(extensions).build()

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
        for ((type, title) in cases) {
            assertHtml(
                "> [!$type]\n> This is content",
                alertHtml(type.lowercase(), title, "<p>This is content</p>\n"),
            )
        }
        assertHtml(
            "> [!note]\n> First paragraph\n>\n> Second **paragraph**",
            alertHtml(
                "note",
                "Note",
                "<p>First paragraph</p>\n<p>Second <strong>paragraph</strong></p>\n",
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
            AlertsExtension.builder()
                .addCustomType("INFO", "Information")
                .addCustomType("NOTE", "Nota")
                .build()
        val customParser = Parser.builder().extensions(listOf(extension)).build()
        val customHtml = HtmlRenderer.builder().extensions(listOf(extension)).build()
        assertEquals(
            alertHtml("info", "Information", "<p>Custom alert</p>\n"),
            customHtml.render(customParser.parse("> [!INFO]\n> Custom alert")),
        )
        assertEquals(
            alertHtml("note", "Nota", "<p>Localized title</p>\n"),
            customHtml.render(customParser.parse("> [!NOTE]\n> Localized title")),
        )
        assertFailsWith<IllegalArgumentException> {
            AlertsExtension.builder().addCustomType("info", "Information")
        }
        assertFailsWith<IllegalArgumentException> {
            AlertsExtension.builder().addCustomType("", "Title")
        }
        assertFailsWith<IllegalArgumentException> {
            AlertsExtension.builder().addCustomType("INFO", "")
        }
        assertFailsWith<IllegalArgumentException> {
            AlertsExtension.builder().setAllowedTypes(emptyMap())
        }
    }

    @Test
    fun customTitlesAndSourceSpans() {
        val customExtensions =
            listOf(AlertsExtension.builder().allowCustomTitles(true).build())
        val customParser =
            Parser.builder()
                .extensions(customExtensions)
                .includeSourceSpans(IncludeSourceSpans.BLOCKS_AND_INLINES)
                .build()
        val customHtml = HtmlRenderer.builder().extensions(customExtensions).build()
        val source = "> [!NOTE] Custom _title_\n> Body text"
        assertEquals(
            alertHtml("note", "Custom <em>title</em>", "<p>Body text</p>\n"),
            customHtml.render(customParser.parse(source)),
        )
        assertEquals(
            alertHtml("important", "See <a href=\"https://example.com\">docs</a> or <code>run()</code>", "<p>Body</p>\n"),
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
        val source = "> [!TIP]\n> Body\n> > [!IMPORTANT]\n> > Nested body"
        assertHtml(
            source,
            alertHtml(
                "tip",
                "Tip",
                "<p>Body</p>\n<blockquote>\n<p>[!IMPORTANT]\nNested body</p>\n</blockquote>\n",
            ),
        )

        val nestedExtensions =
            listOf(AlertsExtension.builder().allowNestedAlerts(true).build())
        val nestedParser = Parser.builder().extensions(nestedExtensions).build()
        val nestedHtml = HtmlRenderer.builder().extensions(nestedExtensions).build()
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
        val renderer = MarkdownRenderer.builder().extensions(extensions).build()
        for (
            input in
                listOf(
                    "> [!WARNING]\n> Be careful\n",
                    "> [!NOTE]\n> Note\n",
                    "> [!TIP]\n> Items:\n> \n> - First\n> - Second\n",
                    "> [!NOTE]\n",
                    "- List\n\n> [!NOTE]\n\n- List\n",
                )
        ) {
            assertEquals(input, renderer.render(parser.parse(input)))
        }
        assertEquals(
            "> [!NOTE]\n> Content\n",
            renderer.render(parser.parse("> [!note]\n> Content\n")),
        )

        val customExtensions =
            listOf(AlertsExtension.builder().allowCustomTitles(true).build())
        val customParser = Parser.builder().extensions(customExtensions).build()
        val customRenderer = MarkdownRenderer.builder().extensions(customExtensions).build()
        val titled = "> [!WARNING] Custom _title **with `formatting`**_\n> Be careful\n"
        assertEquals(titled, customRenderer.render(customParser.parse(titled)))
    }

    @Test
    fun textRenderingModes() {
        val source = "> [!NOTE]\n> First paragraph\n>\n> Second paragraph"
        assertText(source, LineBreakRendering.COMPACT, "Note\nFirst paragraph\nSecond paragraph")
        assertText(
            source,
            LineBreakRendering.SEPARATE_BLOCKS,
            "Note\n\nFirst paragraph\n\nSecond paragraph",
        )
        assertText(source, LineBreakRendering.STRIP, "Note: First paragraph Second paragraph")
    }

    private fun assertText(source: String, mode: LineBreakRendering, expected: String) {
        val renderer =
            TextContentRenderer.builder()
                .extensions(extensions)
                .lineBreakRendering(mode)
                .build()
        assertEquals(expected, renderer.render(parser.parse(source)))
    }

    private fun assertHtml(source: String, expected: String) {
        assertEquals(expected, html.render(parser.parse(source)))
    }

    private fun alertHtml(type: String, title: String, body: String): String =
        "<div class=\"markdown-alert markdown-alert-$type\" data-alert-type=\"$type\">\n" +
            "<p class=\"markdown-alert-title\">$title</p>\n" +
            body +
            "</div>\n"
}
