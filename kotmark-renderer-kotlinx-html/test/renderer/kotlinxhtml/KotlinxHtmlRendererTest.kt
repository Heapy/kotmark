package io.heapy.kotmark.renderer.kotlinxhtml

import io.heapy.kotmark.node.Document
import io.heapy.kotmark.node.Text
import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.typedNodeRenderer
import io.heapy.kotmark.renderer.html.AttributeProvider
import io.heapy.kotmark.renderer.html.UrlResolver
import kotlinx.html.div
import kotlinx.html.span
import kotlinx.html.stream.createHTML
import kotlin.test.*

class KotlinxHtmlRendererTest {
    private val parser = parser()
    private val renderer = kotlinxHtmlRenderer()

    @Test
    fun coreBlocksAndInlineContent() {
        assertEquals("<h1>Title</h1><p>Hello <em>world</em> <strong>bold</strong> <code>x&lt;y</code>.</p><hr>",
            renderer.render(parser.parse("# Title\n\nHello *world* **bold** `x<y`.\n\n---")))
        assertEquals("<blockquote><p>quote</p></blockquote>", renderer.render(parser.parse("> quote")))
        assertEquals("<ol start=\"3\"><li>three</li><li>four</li></ol>", renderer.render(parser.parse("3. three\n4. four")))
        assertEquals("<ul><li><p>a</p></li><li><p>b</p></li></ul>", renderer.render(parser.parse("- a\n\n- b")))
        assertEquals("<ul><li>a<ul><li>b</li></ul></li></ul>", renderer.render(parser.parse("- a\n  - b")))
    }

    @Test
    fun codeAndLineBreaksPreserveContent() {
        assertEquals("<pre><code class=\"language-kotlin\">&lt;x&gt; &amp; y\n</code></pre>",
            renderer.render(parser.parse("```kotlin title\n<x> & y\n```")))
        assertEquals("<pre><code>x\n</code></pre>", renderer.render(parser.parse("    x")))
        assertEquals("<p>a\nb<br>\nc</p>", renderer.render(parser.parse("a\nb  \nc")))
        assertEquals("<p><code>\\&amp;lt;</code></p>", renderer.render(parser.parse("`\\&lt;`")))
        assertEquals("<p>a<br>b</p>", kotlinxHtmlRenderer { softbreak = "<br>" }.render(parser.parse("a\nb")))
    }

    @Test
    fun htmlAndUrlDefaultsAreExplicit() {
        assertEquals("<p>&lt;script&gt;bad()&lt;/script&gt;</p>", renderer.render(parser.parse("<script>bad()</script>")))
        assertEquals("<p>&lt;b&gt;x&lt;/b&gt;</p>", renderer.render(parser.parse("<b>x</b>")))
        assertEquals("<p><a rel=\"nofollow\" href=\"\">click</a></p>", renderer.render(parser.parse("[click](javascript:alert)")))
        val trusted = kotlinxHtmlRenderer { escapeHtml = false; sanitizeUrls = false }
        assertEquals("<p><b>x</b></p>\n", trusted.render(parser.parse("<b>x</b>")))
        assertEquals("<p><a href=\"/a&amp;b\" title=\"title\">link</a> <img src=\"img\" alt=\"a b\" title=\"t\"></p>\n",
            trusted.render(parser.parse("[link](/a&b \"title\") ![*a* `b`](img \"t\")")))
    }

    @Test
    fun resolverRunsBeforeSanitizingAndEncodingAndDoesNotMutateAst() {
        val document = parser.parse("[a](relative) ![b](relative)")
        val resolved = kotlinxHtmlRenderer {
            urlResolver = UrlResolver { _, _ -> "https://example.test/a b" }
            percentEncodeUrls = true
        }
        val html = resolved.render(document)
        assertTrue(html.contains("href=\"https://example.test/a%20b\""))
        assertTrue(html.contains("src=\"https://example.test/a%20b\""))
        assertTrue(renderer.render(document).contains("href=\"relative\""))
        val unsafe = kotlinxHtmlRenderer { urlResolver = UrlResolver { _, _ -> "javascript:bad" } }
        assertTrue(unsafe.render(document).contains("href=\"\""))
    }

    @Test
    fun insertsIntoAnOpenDslContainerWithoutFinalizingIt() {
        val html = createHTML(prettyPrint = false).div {
            span { +"before" }
            renderer.render(parser.parse("body"), this)
            span { +"after" }
        }
        assertEquals("<div><span>before</span><p>body</p><span>after</span></div>", html)
        val buffer = StringBuilder("prefix")
        renderer.render(parser.parse("body"), buffer)
        assertEquals("prefix<p>body</p>", buffer.toString())
    }

    @Test
    fun customizationUsesDslAndFirstRegisteredHandlerWins() {
        val custom = kotlinxHtmlRenderer {
            nodeRenderer<Text> { node ->
                element(node, "span") { span { +node.literal.orEmpty().uppercase() } }
            }
            nodeRendererFactory { context -> typedNodeRenderer<Text> { context.text("factory") } }
            nodeRenderer<Text> { text("last") }
            attributeProviderFactory { AttributeProvider { _, tag, attributes -> if (tag == "span") attributes["class"] = "label" } }
        }
        repeat(2) {
            assertEquals("<p><span class=\"label\"><span>CUSTOM</span></span></p>", custom.render(parser.parse("custom")))
        }
    }

    @Test
    fun typedFactoryCreatesFreshMutableStateForEachRender() {
        val custom = kotlinxHtmlRenderer {
            nodeRendererFactory { context ->
                var count = 0
                typedNodeRenderer<Text> { node -> context.text("${++count}:${node.literal}") }
            }
        }
        val document = Document().apply { appendChild(Text("a")); appendChild(Text("b")) }
        repeat(2) { assertEquals("1:a2:b", custom.render(document)) }
    }

    @Test
    fun configurationIsSnapshotted() {
        lateinit var configuration: KotlinxHtmlRenderer.Configuration
        val custom = kotlinxHtmlRenderer { configuration = this; omitSingleParagraphP = true }
        configuration.omitSingleParagraphP = false
        configuration.attributeProviderFactory { AttributeProvider { _, _, attrs -> attrs["class"] = "late" } }
        configuration.nodeRenderer<Text> { text("late") }
        assertEquals("text", custom.render(parser.parse("text")))
        assertEquals("<p>a</p><p>b</p>", custom.render(parser.parse("a\n\nb")))
    }
}
