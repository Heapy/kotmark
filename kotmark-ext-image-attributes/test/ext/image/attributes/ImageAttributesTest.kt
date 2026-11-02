package io.heapy.kotmark.ext.image.attributes

import io.heapy.kotmark.Extension
import io.heapy.kotmark.ext.image.attributes.internal.ImageAttributesDelimiterProcessor
import io.heapy.kotmark.node.Image
import io.heapy.kotmark.node.Text
import io.heapy.kotmark.parser.delimiter.DelimiterRun
import io.heapy.kotmark.node.Paragraph
import io.heapy.kotmark.node.SourceSpan
import io.heapy.kotmark.parser.IncludeSourceSpans
import io.heapy.kotmark.parser.Parser
import io.heapy.kotmark.renderer.html.HtmlRenderer
import kotlin.test.Test
import kotlin.test.assertEquals

class ImageAttributesTest {
    private val extensions: List<Extension> = listOf(ImageAttributesExtension.create())
    private val parser = Parser.builder().extensions(extensions).build()
    private val renderer = HtmlRenderer.builder().extensions(extensions).build()

    @Test
    fun processorAcceptsTrailingWhitespace() {
        val image = Image("url", "")
        val opening = Text("{")
        val content = Text("width=5   ")
        val closing = Text("}")
        val paragraph = Paragraph()
        for (node in listOf(image, opening, content, closing)) paragraph.appendChild(node)
        val run = object : DelimiterRun {
            override fun canOpen() = true
            override fun canClose() = true
            override fun length() = 1
            override fun originalLength() = 1
            override val opener = opening
            override val closer = closing
            override fun getOpeners(length: Int) = listOf(opening)
            override fun getClosers(length: Int) = listOf(closing)
        }
        val processor = ImageAttributesDelimiterProcessor()
        assertEquals(1, processor.process(run, run))
        assertEquals("5", (image.firstChild as ImageAttributes).attributes["width"])
    }

    @Test
    fun trailingEmptyPartsFollowJavaSplitSemantics() {
        assertHtml("![text](/url.png){}", "<p><img src=\"/url.png\" alt=\"text\" />{}</p>\n")
        assertHtml("![text](/url.png){width=}", "<p><img src=\"/url.png\" alt=\"text\" />{width=}</p>\n")
        assertHtml("![text](/url.png){height=5 width=}", "<p><img src=\"/url.png\" alt=\"text\" />{height=5 width=}</p>\n")
        assertHtml("![text](/url.png){width=5   }", "<p><img src=\"/url.png\" alt=\"text\" />{width=5   }</p>\n")
        assertHtml("![text](/url.png){width=5=}", "<p><img src=\"/url.png\" alt=\"text\" width=\"5\" /></p>\n")
    }

    @Test
    fun supportedAttributes() {
        assertHtml(
            "![text](/url.png){height=5}",
            "<p><img src=\"/url.png\" alt=\"text\" height=\"5\" /></p>\n",
        )
        assertHtml(
            "![text](/url.png){height=5 width=6}",
            "<p><img src=\"/url.png\" alt=\"text\" height=\"5\" width=\"6\" /></p>\n",
        )
        assertHtml(
            "![text](/url.png){height=99px   width=100px}",
            "<p><img src=\"/url.png\" alt=\"text\" height=\"99px\" width=\"100px\" /></p>\n",
        )
        assertHtml(
            "![text](/url.png){height=4.8 width=3.14}",
            "<p><img src=\"/url.png\" alt=\"text\" height=\"4.8\" width=\"3.14\" /></p>\n",
        )
        assertHtml(
            "![text](/url.png){Width=18 HeIgHt=1001}",
            "<p><img src=\"/url.png\" alt=\"text\" Width=\"18\" HeIgHt=\"1001\" /></p>\n",
        )
        assertHtml(
            "![text](/url.png){height=green width=blue}",
            "<p><img src=\"/url.png\" alt=\"text\" height=\"green\" width=\"blue\" /></p>\n",
        )
    }

    @Test
    fun invalidAttributesRemainLiteral() {
        assertHtml(
            "![text](/url.png){{height=5}}",
            "<p><img src=\"/url.png\" alt=\"text\" />{{height=5}}</p>\n",
        )
        assertHtml("![text](/url.png){", "<p><img src=\"/url.png\" alt=\"text\" />{</p>\n")
        assertHtml(
            "![text](/url.png){j=502 K=101 img=2 url=5}",
            "<p><img src=\"/url.png\" alt=\"text\" />{j=502 K=101 img=2 url=5}</p>\n",
        )
        assertHtml(
            "![foo](/url.png){height=3 invalid}\n",
            "<p><img src=\"/url.png\" alt=\"foo\" />{height=3 invalid}</p>\n",
        )
        assertHtml(
            "![foo](/url.png){height=3 *test*}\n",
            "<p><img src=\"/url.png\" alt=\"foo\" />{height=3 <em>test</em>}</p>\n",
        )
        assertHtml(
            "![text](/url.png){height}",
            "<p><img src=\"/url.png\" alt=\"text\" />{height}</p>\n",
        )
    }

    @Test
    fun repeatedAndEscapedValues() {
        assertHtml(
            "![text](/url.png){height=4 height=5 width=1 height=6}",
            "<p><img src=\"/url.png\" alt=\"text\" height=\"6\" width=\"1\" /></p>\n",
        )
        assertHtml(
            "![text](/url.png){height=<img}",
            "<p><img src=\"/url.png\" alt=\"text\" height=\"&lt;img\" /></p>\n",
        )
        assertHtml(
            "![text](/url.png){height=\"\"img}",
            "<p><img src=\"/url.png\" alt=\"text\" height=\"&quot;&quot;img\" /></p>\n",
        )
    }

    @Test
    fun imageAltText() {
        assertHtml(
            "![Android SDK Manager](/contrib/android-sdk-manager.png){height=502 width=101}",
            "<p><img src=\"/contrib/android-sdk-manager.png\" alt=\"Android SDK Manager\" height=\"502\" width=\"101\" /></p>\n",
        )
        assertHtml(
            "![foo\nbar](/url){height=101 width=202}\n",
            "<p><img src=\"/url\" alt=\"foo\nbar\" height=\"101\" width=\"202\" /></p>\n",
        )
        assertHtml(
            "![foo  \nbar](/url){height=506 width=1}\n",
            "<p><img src=\"/url\" alt=\"foo\nbar\" height=\"506\" width=\"1\" /></p>\n",
        )
        assertHtml(
            "![foo &auml;](/url){height=99 width=100}\n",
            "<p><img src=\"/url\" alt=\"foo ä\" height=\"99\" width=\"100\" /></p>\n",
        )
    }

    @Test
    fun ordinaryTextIsUnchanged() {
        assertHtml("x{height=3 width=4}\n", "<p>x{height=3 width=4}</p>\n")
        assertHtml("x {height=3 width=4}\n", "<p>x {height=3 width=4}</p>\n")
        assertHtml("\\documentclass[12pt]{article}\n", "<p>\\documentclass[12pt]{article}</p>\n")
        assertHtml("some *text*{height=3 width=4}\n", "<p>some <em>text</em>{height=3 width=4}</p>\n")
        assertHtml("{NN} text", "<p>{NN} text</p>\n")
        assertHtml("{}", "<p>{}</p>\n")
    }

    @Test
    fun sourceSpansRemainMergedForPlainText() {
        val spansParser =
            Parser.builder()
                .extensions(extensions)
                .includeSourceSpans(IncludeSourceSpans.BLOCKS_AND_INLINES)
                .build()
        val paragraph = spansParser.parse("x{height=3 width=4}\n").firstChild as Paragraph
        assertEquals(
            listOf(SourceSpan.of(0, 0, 0, 19)),
            paragraph.firstChild?.getSourceSpans(),
        )
    }

    private fun assertHtml(source: String, expected: String) {
        assertEquals(expected, renderer.render(parser.parse(source)))
    }
}
