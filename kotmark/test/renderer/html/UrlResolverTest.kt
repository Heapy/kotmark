package io.heapy.kotmark.renderer.html

import io.heapy.kotmark.node.Image
import io.heapy.kotmark.node.Link
import io.heapy.kotmark.node.Node
import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.NodeRenderer
import io.heapy.kotmark.renderer.markdown.markdownRenderer
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UrlResolverTest {
    @Test
    fun resolvesRelativeReferences() {
        val resolver = BaseUriUrlResolver("https://example.org/docs/guide/page.md?old#previous", true)
        val expected = mapOf(
            "../images/example.png" to "https://example.org/docs/images/example.png",
            "next.md" to "https://example.org/docs/guide/next.md",
            "./next.md" to "https://example.org/docs/guide/next.md",
            "/images/a.png" to "https://example.org/images/a.png",
            "//cdn.example.org/a/../b.png" to "https://cdn.example.org/b.png",
            "?new" to "https://example.org/docs/guide/page.md?new",
            "?" to "https://example.org/docs/guide/page.md?",
            "#section" to "https://example.org/docs/guide/page.md?old#section",
            "#" to "https://example.org/docs/guide/page.md?old#",
            "" to "https://example.org/docs/guide/page.md?old",
            "." to "https://example.org/docs/guide/",
            ".." to "https://example.org/docs/",
            "../../../../x" to "https://example.org/x",
            "/../x" to "https://example.org/x",
            "a/./b/../c/." to "https://example.org/docs/guide/a/c/",
            "a/.." to "https://example.org/docs/guide/",
            "a//b" to "https://example.org/docs/guide/a//b",
            "a//../b" to "https://example.org/docs/guide/a/b",
            "a...?q/../x#f/./y" to "https://example.org/docs/guide/a...?q/../x#f/./y",
            "%2e%2e/a%2Fb" to "https://example.org/docs/guide/%2e%2e/a%2Fb",
            "space ä.png" to "https://example.org/docs/guide/space ä.png",
        )
        for ([reference, url] in expected) {
            assertEquals(url, resolver.resolveUrl(Link(reference, null), reference), reference)
            assertEquals(url, resolver.resolveUrl(Image(reference, null), reference), reference)
        }
    }

    @Test
    fun handlesDirectoryHostAndFileBases() {
        val cases = mapOf(
            "https://example.org/docs/guide/" to "https://example.org/docs/images/example.png",
            "https://example.org" to "https://example.org/images/example.png",
            "file:///docs/guide/" to "file:///docs/images/example.png",
            "https://[::1]:8080/docs/guide/" to "https://[::1]:8080/docs/images/example.png",
        )
        for ([base, expected] in cases) {
            val url = "../images/example.png"
            assertEquals(expected, BaseUriUrlResolver(base).resolveUrl(Image(url, null), url))
        }
    }

    @Test
    fun preservesLocalAnchorsAndAbsoluteUrls() {
        val resolver = BaseUriUrlResolver("https://example.org/docs/")
        for (url in listOf("#section", "#", "https://elsewhere.org/a/../b", "mailto:a@example.org", "data:image/png;base64,abc", "custom+v1:a", "https:page")) {
            assertEquals(url, resolver.resolveUrl(Link(url, null), url))
        }
        assertEquals(
            "https://example.org/docs/next#section",
            resolver.resolveUrl(Link("next#section", null), "next#section"),
        )
    }

    @Test
    fun requiresAbsoluteHierarchicalBase() {
        for (base in listOf("", "docs/", "/docs/", "//example.org/docs/", "mailto:a@example.org", "1https://example.org")) {
            assertFailsWith<IllegalArgumentException>(base) { BaseUriUrlResolver(base) }
        }
    }

    @Test
    fun localAnchorOptionAppliesToRenderedLinksAndImages() {
        val document = parser().parse("[section](#section) ![svg](#icon)")
        val local = htmlRenderer { urlResolver = BaseUriUrlResolver("https://example.org/page") }
        val resolved = htmlRenderer { urlResolver = BaseUriUrlResolver("https://example.org/page", true) }
        assertEquals("<p><a href=\"#section\">section</a> <img src=\"#icon\" alt=\"svg\" /></p>\n", local.render(document))
        assertEquals(
            "<p><a href=\"https://example.org/page#section\">section</a> " +
                "<img src=\"https://example.org/page#icon\" alt=\"svg\" /></p>\n",
            resolved.render(document),
        )
    }

    @Test
    fun rendersLinksImagesReferencesAndAutolinksWithoutChangingAst() {
        val document = parser().parse("[page][p] ![image](../images/example.png) <https://other.org>\n\n[p]: next.md")
        val markdownBefore = markdownRenderer().render(document)
        val paragraph = requireNotNull(document.firstChild)
        val link = paragraph.firstChild as Link
        val image = link.next?.next as Image
        val renderer = htmlRenderer { urlResolver = BaseUriUrlResolver("https://example.org/docs/guide/") }
        val expected = "<p><a href=\"https://example.org/docs/guide/next.md\">page</a> " +
            "<img src=\"https://example.org/docs/images/example.png\" alt=\"image\" /> " +
            "<a href=\"https://other.org\">https://other.org</a></p>\n"
        assertEquals(expected, renderer.render(document))
        assertEquals(expected, renderer.render(document))
        assertEquals("next.md", link.destination)
        assertEquals("../images/example.png", image.destination)
        assertEquals(markdownBefore, markdownRenderer().render(document))
        assertEquals(
            "<p><a href=\"next.md\">page</a> <img src=\"../images/example.png\" alt=\"image\" /> " +
                "<a href=\"https://other.org\">https://other.org</a></p>\n",
            htmlRenderer().render(document),
        )
    }

    @Test
    fun customResolverCanRewritePagesAndUseImageCdn() {
        val pages = BaseUriUrlResolver("https://example.org/docs/")
        val images = BaseUriUrlResolver("https://cdn.example.org/assets/")
        val renderer = htmlRenderer {
            urlResolver = UrlResolver { node, destination ->
                if (node is Image) images.resolveUrl(node, destination)
                else pages.resolveUrl(node, destination.removeSuffix(".md") + ".html")
            }
        }
        assertEquals(
            "<p><a href=\"https://example.org/docs/next.html\">page</a> " +
                "<img src=\"https://cdn.example.org/assets/a.png\" alt=\"image\" /></p>\n",
            renderer.render(parser().parse("[page](next.md) ![image](a.png)")),
        )
    }

    @Test
    fun sanitizesAndEncodesResolvedUrlsThenEscapesAttributes() {
        val renderer = htmlRenderer {
            sanitizeUrls = true
            percentEncodeUrls = true
            urlResolver = UrlResolver { _, destination ->
                if (destination == "unsafe") "javascript:alert(1)"
                else "https://example.org/ä b%20c?q=1&x=2"
            }
        }
        assertEquals(
            "<p><a rel=\"nofollow\" href=\"\">bad</a> <img src=\"\" alt=\"bad\" /> " +
                "<a rel=\"nofollow\" href=\"https://example.org/%C3%A4%20b%20c?q=1&amp;x=2\">ok</a> " +
                "<img src=\"https://example.org/%C3%A4%20b%20c?q=1&amp;x=2\" alt=\"ok\" /></p>\n",
            renderer.render(parser().parse("[bad](unsafe) ![bad](unsafe) [ok](safe) ![ok](safe)")),
        )
    }

    @Test
    fun snapshotsResolverAndExposesItToCustomRenderers() {
        lateinit var configuration: HtmlRenderer.Configuration
        val renderer = htmlRenderer {
            configuration = this
            urlResolver = BaseUriUrlResolver("https://example.org/docs/")
            nodeRendererFactory { context ->
                object : NodeRenderer {
                    override val nodeTypes: Set<KClass<out Node>> = setOf(Link::class)

                    override fun render(node: Node) {
                        context.writer.text(context.resolveUrl(node, (node as Link).destination.orEmpty()))
                    }
                }
            }
        }
        configuration.urlResolver = UrlResolver { _, _ -> "changed" }
        assertEquals("<p>https://example.org/docs/page.md</p>\n", renderer.render(parser().parse("[page](page.md)")))
    }
}
