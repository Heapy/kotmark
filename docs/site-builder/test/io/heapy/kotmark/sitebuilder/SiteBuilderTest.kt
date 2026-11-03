package io.heapy.kotmark.sitebuilder

import io.heapy.kotmark.node.Heading
import io.heapy.kotmark.node.Node
import io.heapy.kotmark.node.Text
import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.html.htmlRenderer
import java.nio.file.Path
import org.jsoup.Jsoup
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class SiteBuilderTest {
    @Test
    fun `landing page tree and HTML match the displayed Markdown`() {
        val page = Jsoup.parse(repositoryRoot().resolve("docs/site/index.html").readText())
        val source = requireNotNull(page.selectFirst(".specimen-source code")).wholeText()
        val document = parser().parse(source)
        val tree = requireNotNull(page.selectFirst("[data-preview-panel=tree] code")).wholeText()
        val html = requireNotNull(page.selectFirst("[data-preview-panel=html] code")).wholeText()

        fun label(node: Node): String = when (node) {
            is Heading -> "Heading(level = ${node.level})"
            is Text -> "Text(\"${node.literal}\")"
            else -> requireNotNull(node::class.simpleName)
        }
        val outline = buildString {
            append(label(document))
            fun children(parent: Node, prefix: String) {
                var child = parent.firstChild
                while (child != null) {
                    val node = child
                    val last = node.next == null
                    append('\n').append(prefix).append(if (last) "└─ " else "├─ ").append(label(node))
                    children(node, prefix + if (last) "   " else "│  ")
                    child = node.next
                }
            }
            children(document, "")
        }
        assertEquals(outline, tree)
        assertEquals(htmlRenderer().render(document).trimEnd('\n'), html)
    }

    @Test
    fun `manifest pages render with source-backed code and links`() {
        val repository = repositoryRoot()
        val output = repository.resolve("build/site-builder-test")
        buildSite(repository, output)

        val autolink = output.resolve("extensions/autolink.html").readText()
        assertContains(autolink, "data-snippet=\"docs/examples/src/io/heapy/kotmark/docs/examples/extensions/Autolink.kt#autolink\"")
        assertContains(autolink, "class=\"io-pair\"")
        assertContains(autolink, "gfm-differences")
        assertFalse(autolink.contains("{#gfm-differences}"))

        val extensions = output.resolve("extensions/index.html").readText()
        assertContains(extensions, "href=\"autolink.html\"")
        assertFalse(Regex("href=\"[^\"]+\\.md(?:[?#\"]|$)").containsMatchIn(extensions))

        assertContains(extensions, "class=\"extension-table-wrap\"")
        assertContains(extensions, "scope=\"col\"")
        assertContains(extensions, "scope=\"row\"")

        val frontMatter = Jsoup.parse(output.resolve("extensions/yaml-front-matter.html").readText())
        val panels = frontMatter.select("#example .io-panel")
        assertEquals(
            listOf("INPUT · Markdown", "METADATA · Kotlin map", "OUTPUT · HTML"),
            panels.map { it.selectFirst(".source-label")!!.text() },
        )
        assertEquals("text", panels[1].selectFirst("code")!!.attr("data-language"))
        assertEquals("{title=[Release notes], tags=[kotlin, markdown]}", panels[1].selectFirst("code")!!.wholeText())
        assertEquals("html", panels[2].selectFirst("code")!!.attr("data-language"))
        assertEquals("<h1>Hello</h1>", panels[2].selectFirst("code")!!.wholeText())
        assertEquals("<h1>Hello</h1>\n", renderExample("yaml-front-matter", panels[0].selectFirst("code")!!.wholeText(), repository))

        val credits = output.resolve("credits.html").readText()
        assertContains(credits, "data-upstream=\"MIT notice for autolink\"")
        assertContains(credits, "Copyright (c) 2015 Robin Stocker")
    }

    @Test
    fun `unknown examples and source regions fail loudly`() {
        val repository = repositoryRoot()

        assertFailsWith<IllegalStateException> {
            renderExample("not-registered", "", repository)
        }
        assertFailsWith<IllegalArgumentException> {
            readSnippet(repository, "docs/examples/src/io/heapy/kotmark/docs/examples/extensions/Autolink.kt#missing")
        }
    }

    @Test
    fun `headings inside code fences do not affect generated IDs`() {
        val ids = headingIds(
            """
            ## Example
            ````markdown
            ## This is code
            ```
            ## Still code
            ````
            ## Actual section
            """.trimIndent(),
            mutableSetOf(),
        )

        assertEquals(listOf("example", "actual-section"), ids)
    }

    private fun repositoryRoot(): Path {
        var directory = Path.of("").toAbsolutePath().normalize()
        while (!directory.resolve("docs/pages/site.yml").isRegularFile()) {
            directory = requireNotNull(directory.parent) { "Could not locate docs/pages/site.yml from ${Path.of("").toAbsolutePath()}" }
        }
        return directory
    }
}
