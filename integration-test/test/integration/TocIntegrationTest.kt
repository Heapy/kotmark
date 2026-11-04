package io.heapy.kotmark.integration

import io.heapy.kotmark.ext.autolink.autolink
import io.heapy.kotmark.ext.emoji.emoji
import io.heapy.kotmark.ext.footnotes.footnotes
import io.heapy.kotmark.ext.gfm.strikethrough.strikethrough
import io.heapy.kotmark.ext.heading.anchor.headingAnchor
import io.heapy.kotmark.ext.highlight.highlight
import io.heapy.kotmark.ext.toc.toc
import io.heapy.kotmark.extensions
import io.heapy.kotmark.node.Heading
import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.html.AttributeProvider
import io.heapy.kotmark.renderer.html.htmlRenderer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TocIntegrationTest {
    @Test
    fun headingAnchorsMatchInEitherRegistrationOrder() {
        for (anchorsFirst in listOf(true, false)) {
            val extensions = extensions {
                if (anchorsFirst) headingAnchor { idPrefix = "section-"; idSuffix = "-end" }
                toc { headingLevels = 2..6 }
                if (!anchorsFirst) headingAnchor { idPrefix = "section-"; idSuffix = "-end" }
            }
            val document = parser { extensions(extensions) }.parse("[TOC]\n# Ж\n## Ж\n## Ж-1\n## Ж")
            val renderer = htmlRenderer { extensions(extensions) }
            val result = renderer.render(document)
            assertFalse(result.contains("href=\"#section-%D0%B6-end\""))
            for (suffix in listOf("1", "1-1", "2")) {
                assertTrue(result.contains("href=\"#section-%D0%B6-$suffix-end\""), result)
                assertTrue(result.contains("id=\"section-ж-$suffix-end\""), result)
            }
            assertEquals(result, renderer.render(document))
        }
    }

    @Test
    fun customIdProviderOverridesHeadingAnchorsOncePerHeading() {
        val extensions = extensions { toc(); headingAnchor() }
        var count = 0
        val renderer = htmlRenderer {
            extensions(extensions)
            attributeProviderFactory {
                AttributeProvider { node, _, attributes ->
                    if (node is Heading) attributes["id"] = "custom-${++count}"
                }
            }
        }
        val result = renderer.render(parser { extensions(extensions) }.parse("[TOC]\n# A\n## A"))
        assertEquals(2, count)
        for (id in 1..2) {
            assertTrue(result.contains("href=\"#custom-$id\""))
            assertTrue(result.contains("id=\"custom-$id\""))
        }
    }

    @Test
    fun inlineExtensionsAndFootnoteDefinitionsDoNotCreateRecursiveContents() {
        val extensions = extensions { toc(); headingAnchor(); emoji(); highlight(); strikethrough(); autolink(); footnotes() }
        val source = "[TOC]\n# ==Good== ~~old~~ :heart: https://example.com[^note]\n\n" +
            "[^note]: # Hidden heading\n\n    [TOC]"
        val result = htmlRenderer { extensions(extensions) }.render(parser { extensions(extensions) }.parse(source))
        val list = result.substringBefore("<h1")
        assertEquals(1, Regex("<a ").findAll(list).count())
        assertTrue(list.contains(">Good old ❤️ https://example.com</a>"), list)
        assertFalse(list.contains("Hidden heading"))
        assertTrue(result.contains("<mark>Good</mark> <del>old</del> ❤️"))
    }

    @Test
    fun generatedLinksUseNormalUrlPipeline() {
        val extensions = extensions { toc() }
        val document = parser { extensions(extensions) }.parse("[TOC]\n# A")
        val renderer = htmlRenderer {
            extensions(extensions)
            sanitizeUrls = true
            urlResolver = { _, destination -> if (destination == "#a") "javascript:alert(1)" else destination }
        }
        val result = renderer.render(document)
        assertTrue(result.contains("href=\"\""), result)
        assertFalse(result.contains("javascript:"))
    }
}
