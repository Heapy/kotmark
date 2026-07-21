package org.commonmark.renderer.html

import org.commonmark.node.Code
import org.commonmark.node.Document
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.Image
import org.commonmark.node.Link
import org.commonmark.node.Node
import org.commonmark.node.Paragraph
import org.commonmark.parser.Parser
import org.commonmark.renderer.NodeRenderer
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
            render(inline, HtmlRenderer.builder().escapeHtml(true).build()),
        )

        val block = "<div id='foo'>block &amp;</div>"
        assertEquals("<div id='foo'>block &amp;</div>\n", render(block))
        assertEquals(
            "<p>&lt;div id='foo'&gt;block &amp;amp;&lt;/div&gt;</p>\n",
            render(block, HtmlRenderer.builder().escapeHtml(true).build()),
        )
    }

    @Test
    fun escapesTextAndAttributes() {
        assertEquals(
            "<p>escaping: &amp; &lt; &gt; &quot; '</p>\n",
            render("escaping: & < > \" '"),
        )

        val paragraph = Paragraph()
        paragraph.appendChild(Link("&colon;", null))
        assertEquals(
            "<p><a href=\"&amp;colon;\"></a></p>\n",
            HtmlRenderer.builder().build().render(paragraph),
        )
    }

    @Test
    fun sanitizesUrls() {
        val renderer =
            HtmlRenderer.builder()
                .sanitizeUrls(true)
                .urlSanitizer(DefaultUrlSanitizer())
                .build()

        assertEquals(
            "<p><a rel=\"nofollow\" href=\"https://example.com\">safe</a></p>\n",
            render("[safe](https://example.com)", renderer),
        )
        assertEquals(
            "<p><a rel=\"nofollow\" href=\"\">unsafe</a></p>\n",
            render("[unsafe](javascript:alert)", renderer),
        )
        assertEquals(
            "<p><img src=\"\" alt=\"unsafe\" /></p>\n",
            render("![unsafe](ftp://example.com/a.png)", renderer),
        )
    }

    @Test
    fun percentEncodesUrls() {
        val renderer = HtmlRenderer.builder().percentEncodeUrls(true).build()
        assertEquals(
            "<p><a href=\"foo%20%C3%A4%5B%5D\">a</a></p>\n",
            render("[a](foo%20ä[])", renderer),
        )
        assertEquals(
            "<p><a href=\"foo%25xx\">a</a></p>\n",
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
        val renderer = HtmlRenderer.builder().attributeProviderFactory(factory).build()

        assertEquals(
            "<pre data-code-block=\"fenced\"><code data-custom=\"evil&quot;\">content\n</code></pre>\n",
            render("```evil\"\ncontent\n```", renderer),
        )
        assertEquals(
            "<p><img src=\"/url\" test=\"hey\" /></p>\n",
            render("![foo](/url)", renderer),
        )
    }

    @Test
    fun customNodeRendererOverridesCoreRenderer() {
        val renderer =
            HtmlRenderer.builder()
                .nodeRendererFactory { context ->
                    object : NodeRenderer {
                        override val nodeTypes: Set<KClass<out Node>> = setOf(Link::class)

                        override fun render(node: Node) {
                            context.writer.text("test")
                        }
                    }
                }
                .build()

        assertEquals("<p>foo test</p>\n", render("foo [bar](/url)", renderer))
    }

    @Test
    fun rendersImageAltTextAndCanOmitSingleParagraph() {
        assertEquals(
            "<p><img src=\"/url\" alt=\"foo\nbar ä link code\" /></p>\n",
            render("![foo\nbar &auml; [link](/other) `code`](/url)"),
        )
        assertEquals(
            "hi <em>there</em>",
            render(
                "hi *there*",
                HtmlRenderer.builder().omitSingleParagraphP(true).build(),
            ),
        )
    }

    @Test
    fun rendersDetachedInlineContents() {
        val parsed = Parser.builder().build().parse("Here is `code`")
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
            HtmlRenderer.builder().build().render(document),
        )
    }

    @Test
    fun htmlWriterAndSanitizerMatchPublicContracts() {
        val output = StringBuilder()
        val writer = HtmlWriter(output)
        writer.tag("input", linkedMapOf("disabled" to null, "value" to "\""), true)
        writer.line()
        assertEquals("<input disabled value=\"&quot;\" />\n", output.toString())

        val sanitizer = DefaultUrlSanitizer(listOf("https"))
        assertEquals("https://example.com", sanitizer.sanitizeLinkUrl(" \nhttps://example.com\t"))
        assertEquals("", sanitizer.sanitizeLinkUrl("HTTP://example.com"))
        assertEquals("/relative", sanitizer.sanitizeImageUrl("/relative"))
    }

    private fun render(
        source: String,
        renderer: HtmlRenderer = HtmlRenderer.builder().build(),
    ): String =
        renderer.render(Parser.builder().build().parse(source))
}
