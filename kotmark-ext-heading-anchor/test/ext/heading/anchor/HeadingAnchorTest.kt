package io.heapy.kotmark.ext.heading.anchor

import io.heapy.kotmark.Extension
import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.html.htmlRenderer
import kotlin.test.Test
import kotlin.test.assertEquals

class HeadingAnchorTest {
    private val parser = parser()
    private val renderer =
        htmlRenderer { extensions(listOf(HeadingAnchorExtension.create())) }

    @Test
    fun generatedSuffixesNeverReuseAnEarlierNaturalId() {
        val generator = idGenerator { prefix = "p-"; suffix = "-s" }
        assertEquals(listOf("p-a-1-s", "p-a-s", "p-a-2-s", "p-a-1-1-s", "p-a-3-s"),
            listOf("a-1", "a", "a", "a-1", "a").map(generator::generateId))
    }

    @Test
    fun supplementaryLettersDigitsAndMarksArePreserved() {
        val text = "\uD840\uDC00\uD835\uDFCE\uD834\uDD65"
        assertHtml("# $text", """<h1 id="$text">$text</h1>""" + "\n")
        assertEquals("id", idGenerator().generateId("\uD83D\uDE00"))
    }

    @Test
    fun headingIds() {
        assertHtml("# Heading here\n", """<h1 id="heading-here">Heading here</h1>""" + "\n")
        assertHtml(
            """
                |Hi there
                |# Heading `here`
                |
            """.trimMargin(),
            """
                |<p>Hi there</p>
                |<h1 id="heading-here">Heading <code>here</code></h1>
                |
            """.trimMargin(),
        )
        assertHtml(
            "# Heading here\n# Heading here",
            """
                |<h1 id="heading-here">Heading here</h1>
                |<h1 id="heading-here-1">Heading here</h1>
                |
            """.trimMargin(),
        )
        assertHtml("# a\u1DC0", "<h1 id=\"a\u1DC0\">a\u1DC0</h1>\n")
        assertHtml("# undertie \u203F", "<h1 id=\"undertie-\u203F\">undertie \u203F</h1>\n")
        assertHtml(
            """
                |# Header
                |# Header
                |# Header-1
            """.trimMargin(),
            """
                |<h1 id="header">Header</h1>
                |<h1 id="header-1">Header</h1>
                |<h1 id="header-1-1">Header-1</h1>
                |
            """.trimMargin(),
        )
        assertHtml(
            "# HEADING here\n# heading here",
            """
                |<h1 id="heading-here">HEADING here</h1>
                |<h1 id="heading-here-1">heading here</h1>
                |
            """.trimMargin(),
        )
    }

    @Test
    fun nestedFormattingAndUnicode() {
        assertHtml(
            "## `h` `e` **l** *l* o",
            """<h2 id="h-e-l-l-o"><code>h</code> <code>e</code> <strong>l</strong> <em>l</em> o</h2>""" + "\n",
        )
        assertHtml(
            "# _hello_ **there**",
            """<h1 id="hello-there"><em>hello</em> <strong>there</strong></h1>""" + "\n",
        )
        assertHtml(
            "# _**Hi there**_",
            """<h1 id="hi-there"><em><strong>Hi there</strong></em></h1>""" + "\n",
        )
        assertHtml("# Hi  There", """<h1 id="hi--there">Hi  There</h1>""" + "\n")
        assertHtml("# bär", """<h1 id="bär">bär</h1>""" + "\n")
        assertHtml(
            "# Product\u036D\u036B",
            "<h1 id=\"product\u036D\u036B\">Product\u036D\u036B</h1>\n",
        )
    }

    @Test
    fun configuration() {
        assertConfigured("# ", "id", "", "", """<h1 id="id"></h1>""" + "\n")
        assertConfigured("# ", "defid", "", "", """<h1 id="defid"></h1>""" + "\n")
        assertConfigured("# text", "", "pre-", "", """<h1 id="pre-text">text</h1>""" + "\n")
        assertConfigured("# text", "", "", "-post", """<h1 id="text-post">text</h1>""" + "\n")
    }

    private fun assertConfigured(
        source: String,
        defaultId: String,
        prefix: String,
        suffix: String,
        expected: String,
    ) {
        val extension: Extension =
            headingAnchorExtension {
                this.defaultId = defaultId
                idPrefix = prefix
                idSuffix = suffix
            }
        val configuredRenderer = htmlRenderer { extensions(listOf(extension)) }
        assertEquals(expected, configuredRenderer.render(parser.parse(source)))
    }

    private fun assertHtml(source: String, expected: String) {
        assertEquals(expected, renderer.render(parser.parse(source)))
    }
}
