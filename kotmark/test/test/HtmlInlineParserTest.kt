package io.heapy.kotmark.test

import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.html.htmlRenderer
import kotlin.test.Test
import kotlin.test.assertEquals

class HtmlInlineParserTest {
    private val parser = parser()
    private val renderer = htmlRenderer()

    @Test
    fun afterFailedAttempt() {
        // An early failure must not stop later inline HTML from being parsed.
        assertRendering("inline <!- <!-- a -->", "<p>inline &lt;!- <!-- a --></p>\n")
        assertRendering("inline <?? <?php ?>", "<p>inline &lt;?? <?php ?></p>\n")
        assertRendering("inline <![x]]> <![CDATA[a]]>", "<p>inline &lt;![x]]&gt; <![CDATA[a]]></p>\n")
        assertRendering("inline <!1> <!bar baz>", "<p>inline &lt;!1&gt; <!bar baz></p>\n")
    }

    @Test
    fun declarationsAllowLowercaseAndNoWhitespace() {
        for (declaration in listOf("<!FOO>", "<!FOO >", "<!FOO 'bar'>", "<!foo bar>", "<!foo>")) {
            assertRendering("inline $declaration", "<p>inline $declaration</p>\n")
        }
    }

    @Test
    fun unterminatedDeclarationsRemainText() {
        assertRendering("inline <!foo <!bar", "<p>inline &lt;!foo &lt;!bar</p>\n")
    }

    @Test
    fun unterminatedConstructDoesNotAffectLaterParagraph() {
        // Reaching the end only proves that the terminator is absent from this inline snippet.
        assertRendering("x <?a\n\nx <?b?>", "<p>x &lt;?a</p>\n<p>x <?b?></p>\n")
        assertRendering("x <!--a\n\nx <!--b-->", "<p>x &lt;!--a</p>\n<p>x <!--b--></p>\n")
        assertRendering("x <![CDATA[a\n\nx <![CDATA[b]]>", "<p>x &lt;![CDATA[a</p>\n<p>x <![CDATA[b]]></p>\n")
        assertRendering("x <!A a\n\nx <!A b>", "<p>x &lt;!A a</p>\n<p>x <!A b></p>\n")
        assertRendering("x <!foo\n\nx <!bar>", "<p>x &lt;!foo</p>\n<p>x <!bar></p>\n")
    }

    private fun assertRendering(source: String, expected: String) {
        assertEquals(expected, renderer.render(parser.parse(source)))
    }
}
