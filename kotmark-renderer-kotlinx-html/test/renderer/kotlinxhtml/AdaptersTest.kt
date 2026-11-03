package io.heapy.kotmark.renderer.kotlinxhtml

import io.heapy.kotmark.Extension
import io.heapy.kotmark.ext.autolink.AutolinkExtension
import io.heapy.kotmark.ext.footnotes.footnotesExtension
import io.heapy.kotmark.ext.front.matter.YamlFrontMatterExtension
import io.heapy.kotmark.ext.gfm.alerts.alertsExtension
import io.heapy.kotmark.ext.gfm.strikethrough.StrikethroughExtension
import io.heapy.kotmark.ext.gfm.tables.TablesExtension
import io.heapy.kotmark.ext.highlight.HighlightExtension
import io.heapy.kotmark.ext.image.attributes.ImageAttributesExtension
import io.heapy.kotmark.ext.ins.InsExtension
import io.heapy.kotmark.ext.math.MathExtension
import io.heapy.kotmark.ext.task.list.items.TaskListItemsExtension
import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.kotlinxhtml.adapters.*
import kotlin.test.*

class AdaptersTest {
    private fun parse(source: String, vararg extensions: Extension) = parser { extensions(extensions.asIterable()) }.parse(source)

    @Test
    fun inlineAdaptersComposeAndMathStaysOpaque() {
        val document = parse("==++~~\$x<y & z\$~~++==", HighlightExtension.create(), InsExtension.create(), StrikethroughExtension.create(), MathExtension.create())
        val renderer = kotlinxHtmlRenderer { adapters(highlightAdapter(), insAdapter(), strikethroughAdapter(), mathAdapter()) }
        assertEquals("<p><mark><ins><del><span class=\"math math-inline\">x&lt;y &amp; z</span></del></ins></mark></p>", renderer.render(document))
        assertEquals("<div class=\"math math-block\">x&lt;y\n</div>", renderer.render(parse("\$\$\nx<y\n\$\$", MathExtension.create())))
        assertFailsWith<IllegalStateException> { kotlinxHtmlRenderer().render(parse("\$x\$", MathExtension.create())) }
    }

    @Test
    fun tablesRenderHeadersAlignmentAndEmptyCells() {
        val document = parse("| a | b |\n| :- | -: |\n| x |", TablesExtension.create())
        assertEquals("<table><thead><tr><th align=\"left\">a</th><th align=\"right\">b</th></tr></thead><tbody><tr><td align=\"left\">x</td><td align=\"right\"></td></tr></tbody></table>",
            kotlinxHtmlRenderer { adapters(tablesAdapter()) }.render(document))
    }

    @Test
    fun alertsRenderCustomInlineTitlesOnceAndNestedBodies() {
        val extension = alertsExtension { customTitlesAllowed = true; nestedAlertsAllowed = true; addCustomType("CUSTOM", "Custom") }
        val renderer = kotlinxHtmlRenderer { adapters(alertsAdapter(mapOf("CUSTOM" to "Custom", "NOTE" to "Note"))) }
        val html = renderer.render(parse("> [!CUSTOM] *Title*\n> Body\n>\n> > [!NOTE]\n> > Nested", extension))
        assertEquals(1, Regex("<em>Title</em>").findAll(html).count())
        assertTrue(html.contains("<p class=\"markdown-alert-title\"><em>Title</em></p><p>Body</p>"))
        assertTrue(html.contains("markdown-alert-note"))
        assertTrue(html.contains("<p>Nested</p>"))
    }

    @Test
    fun tasksAndFrontMatterAndAutolinks() {
        val document = parse("---\ntitle: hi\n---\n- [x] done\n- [ ] later", YamlFrontMatterExtension.create(), TaskListItemsExtension.create())
        assertEquals("<ul><li><input type=\"checkbox\" disabled=\"\" checked=\"\"> done</li><li><input type=\"checkbox\" disabled=\"\"> later</li></ul>",
            kotlinxHtmlRenderer { adapters(yamlFrontMatterAdapter(), taskListItemsAdapter()) }.render(document))
        assertEquals("<p><a rel=\"nofollow\" href=\"https://example.com\">https://example.com</a></p>",
            kotlinxHtmlRenderer().render(parse("https://example.com", AutolinkExtension.create())))
    }

    @Test
    fun imageAttributesDoNotMutateTheTreeAndHeadingCountersReset() {
        val document = parse("# Same\n\n# Same\n\n![alt](x){width=5 height=6}", ImageAttributesExtension.create())
        val renderer = kotlinxHtmlRenderer { adapters(imageAttributesAdapter(), headingAnchorAdapter(idPrefix = "h-")) }
        val expected = "<h1 id=\"h-same\">Same</h1><h1 id=\"h-same-1\">Same</h1><p><img src=\"x\" alt=\"alt\" width=\"5\" height=\"6\"></p>"
        assertEquals(expected, renderer.render(document))
        assertEquals(expected, renderer.render(document))
        assertNotNull(document.lastChild?.firstChild?.lastChild)
    }

    @Test
    fun footnotesNumberByUseIncludeRepeatedBacklinksAndReset() {
        val document = parse("B[^b] A[^a] B[^b] missing[^z]\n\n[^a]: first\n[^b]: second\n[^unused]: hidden", footnotesExtension())
        val renderer = kotlinxHtmlRenderer { adapters(footnotesAdapter()) }
        val html = renderer.render(document)
        assertTrue(html.contains("href=\"#fn-b\" id=\"fnref-b\" data-footnote-ref=\"\">1</a>"))
        assertTrue(html.contains("id=\"fnref-b-2\""))
        assertTrue(html.contains("data-footnote-backref-idx=\"1-2\""))
        assertTrue(html.contains("missing[^z]"))
        assertFalse(html.contains("hidden"))
        assertTrue(html.indexOf("id=\"fn-b\"") < html.indexOf("id=\"fn-a\""))
        assertEquals(html, renderer.render(document))
    }

    @Test
    fun nestedCyclicAndInlineFootnotesTerminateAndRenderNestedAdapters() {
        val renderer = kotlinxHtmlRenderer { adapters(footnotesAdapter(), highlightAdapter()) }
        val html = renderer.render(parse("x[^a] ^[==inline==]\n\n[^a]: A[^b]\n[^b]: B[^a]", footnotesExtension { inlineFootnotes = true }, HighlightExtension.create()))
        assertTrue(html.contains("<mark>inline</mark>"))
        assertEquals(1, Regex("<li id=\"fn-a\">").findAll(html).count())
        assertEquals(1, Regex("<li id=\"fn-b\">").findAll(html).count())
        assertTrue(html.contains("id=\"fn2\""))
    }
}
