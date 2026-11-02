package io.heapy.kotmark.renderer.html

import io.heapy.kotmark.node.Document
import io.heapy.kotmark.node.FencedCodeBlock
import io.heapy.kotmark.node.Image
import io.heapy.kotmark.node.Link
import io.heapy.kotmark.node.Node
import io.heapy.kotmark.node.Paragraph
import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.NodeRenderer
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals

class HtmlRendererTest {
    @Test
    fun rendersCoreMarkdown() {
        val source =
            """
            # Heading

            > quote

            0. zero
            1. one

            ``` kotlin
            val answer = 42
            ```

            [link](/url "title") and ![image](/img)
            """.trimIndent()

        assertEquals(
            """
            <h1>Heading</h1>
            <blockquote>
            <p>quote</p>
            </blockquote>
            <ol start="0">
            <li>zero</li>
            <li>one</li>
            </ol>
            <pre><code class="language-kotlin">val answer = 42
            </code></pre>
            <p><a href="/url" title="title">link</a> and <img src="/img" alt="image" /></p>
            """.trimIndent() + "\n",
            render(source),
        )
    }

    @Test
    fun escapesOrPreservesHtml() {
        val inline = "paragraph with <span id='foo'>inline &amp; html</span>"
        assertEquals(
            "<p>paragraph with <span id='foo'>inline &amp; html</span></p>\n",
            render(inline),
        )
        assertEquals(
            "<p>paragraph with &lt;span id='foo'&gt;inline &amp; html&lt;/span&gt;</p>\n",
            render(inline, htmlRenderer { escapeHtml = true }),
        )

        val block = "<div id='foo'>block &amp;</div>"
        assertEquals("<div id='foo'>block &amp;</div>\n", render(block))
        assertEquals(
            "<p>&lt;div id='foo'&gt;block &amp;amp;&lt;/div&gt;</p>\n",
            render(block, htmlRenderer { escapeHtml = true }),
        )
    }

    @Test
    fun escapesTextAndAttributes() {
        assertEquals(
            "<p>escaping: &amp; &lt; &gt; &quot; '</p>\n",
            render("""escaping: & < > " '"""),
        )

        val paragraph = Paragraph()
        paragraph.appendChild(Link("&colon;", null))
        assertEquals(
            """<p><a href="&amp;colon;"></a></p>""" + "\n",
            htmlRenderer().render(paragraph),
        )
    }

    @Test
    fun sanitizesUrls() {
        val renderer =
            htmlRenderer {
                sanitizeUrls = true
                urlSanitizer = DefaultUrlSanitizer()
            }

        assertEquals(
            """<p><a rel="nofollow" href="https://example.com">safe</a></p>""" + "\n",
            render("[safe](https://example.com)", renderer),
        )
        assertEquals(
            """<p><a rel="nofollow" href="">unsafe</a></p>""" + "\n",
            render("[unsafe](javascript:alert)", renderer),
        )
        assertEquals(
            """<p><img src="" alt="unsafe" /></p>""" + "\n",
            render("![unsafe](ftp://example.com/a.png)", renderer),
        )
    }

    @Test
    fun percentEncodesUrls() {
        val renderer = htmlRenderer { percentEncodeUrls = true }
        assertEquals(
            """<p><a href="foo%20%C3%A4%5B%5D">a</a></p>""" + "\n",
            render("[a](foo%20ä[])", renderer),
        )
        assertEquals(
            """<p><a href="foo%25xx">a</a></p>""" + "\n",
            render("[a](foo%xx)", renderer),
        )
    }

    @Test
    fun appliesAttributeProviders() {
        val factory =
            AttributeProviderFactory {
                AttributeProvider { node, tagName, attributes ->
                    if (node is FencedCodeBlock && tagName == "code") {
                        attributes.remove("class")
                        attributes["data-custom"] = node.info.orEmpty()
                    } else if (node is FencedCodeBlock && tagName == "pre") {
                        attributes["data-code-block"] = "fenced"
                    } else if (node is Image) {
                        attributes.remove("alt")
                        attributes["test"] = "hey"
                    }
                }
            }
        val renderer = htmlRenderer { attributeProviderFactory(factory) }

        assertEquals(
            """
                |<pre data-code-block="fenced"><code data-custom="evil&quot;">content
                |</code></pre>
                |
            """.trimMargin(),
            render("```evil\"\ncontent\n```", renderer),
        )
        assertEquals(
            """<p><img src="/url" test="hey" /></p>""" + "\n",
            render("![foo](/url)", renderer),
        )
    }

    @Test
    fun customNodeRendererOverridesCoreRenderer() {
        val renderer =
            htmlRenderer {
                nodeRendererFactory { context ->
                    object : NodeRenderer {
                        override val nodeTypes: Set<KClass<out Node>> = setOf(Link::class)

                        override fun render(node: Node) {
                            context.writer.text("test")
                        }
                    }
                }
            }

        assertEquals("<p>foo test</p>\n", render("foo [bar](/url)", renderer))
    }

    @Test
    fun rendersImageAltTextAndCanOmitSingleParagraph() {
        assertEquals(
            """
                |<p><img src="/url" alt="foo
                |bar ä link code" /></p>
                |
            """.trimMargin(),
            render("![foo\nbar &auml; [link](/other) `code`](/url)"),
        )
        assertEquals(
            "hi <em>there</em>",
            render(
                "hi *there*",
                htmlRenderer { omitSingleParagraphP = true },
            ),
        )
    }

    @Test
    fun rendersDetachedInlineContents() {
        val parsed = parser().parse("Here is `code`")
        val paragraph = requireNotNull(parsed.firstChild)
        val document = Document()
        var child = paragraph.firstChild
        while (child != null) {
            val current = child
            child = current.next
            document.appendChild(current)
        }

        assertEquals(
            "Here is <code>code</code>",
            htmlRenderer().render(document),
        )
    }

    @Test
    fun htmlWriterAndSanitizerMatchPublicContracts() {
        val output = StringBuilder()
        val writer = HtmlWriter(output)
        writer.tag("input", linkedMapOf("disabled" to null, "value" to "\""), true)
        writer.line()
        assertEquals("""<input disabled value="&quot;" />""" + "\n", output.toString())

        val sanitizer = DefaultUrlSanitizer(listOf("https"))
        assertEquals("https://example.com", sanitizer.sanitizeLinkUrl(" \nhttps://example.com\t"))
        assertEquals("", sanitizer.sanitizeLinkUrl("HTTP://example.com"))
        assertEquals("/relative", sanitizer.sanitizeImageUrl("/relative"))
    }

    private fun render(
        source: String,
        renderer: HtmlRenderer = htmlRenderer(),
    ): String =
        renderer.render(parser().parse(source))
}
