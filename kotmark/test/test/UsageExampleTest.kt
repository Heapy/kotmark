package io.heapy.kotmark.test

import io.heapy.kotmark.node.AbstractVisitor
import io.heapy.kotmark.node.Document
import io.heapy.kotmark.node.Heading
import io.heapy.kotmark.node.Image
import io.heapy.kotmark.node.IndentedCodeBlock
import io.heapy.kotmark.node.Node
import io.heapy.kotmark.node.Text
import io.heapy.kotmark.parser.IncludeSourceSpans
import io.heapy.kotmark.parser.Parser
import io.heapy.kotmark.renderer.NodeRenderer
import io.heapy.kotmark.renderer.html.AttributeProvider
import io.heapy.kotmark.renderer.html.HtmlNodeRendererContext
import io.heapy.kotmark.renderer.html.HtmlRenderer
import io.heapy.kotmark.renderer.markdown.MarkdownRenderer
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals

class UsageExampleTest {
    @Test
    fun parseAndRender() {
        val parser = Parser.builder().build()
        val document = parser.parse("This is *Markdown*")
        val renderer = HtmlRenderer.builder().escapeHtml(true).build()
        assertEquals("<p>This is <em>Markdown</em></p>\n", renderer.render(document))
    }

    @Test
    fun renderToMarkdown() {
        val renderer = MarkdownRenderer.builder().build()
        val document = Document()
        val heading = Heading()
        heading.level = 2
        heading.appendChild(Text("My title"))
        document.appendChild(heading)

        assertEquals("## My title\n", renderer.render(document))
    }

    @Test
    fun visitor() {
        val parser = Parser.builder().build()
        val node = parser.parse("Example\n=======\n\nSome more text")
        val visitor = WordCountVisitor()
        node.accept(visitor)
        assertEquals(4, visitor.wordCount)
    }

    @Test
    fun sourcePositions() {
        val parser = Parser.builder().includeSourceSpans(IncludeSourceSpans.BLOCKS_AND_INLINES).build()

        val source = "foo\n\nbar *baz*"
        val doc = parser.parse(source)
        val emphasis = doc.lastChild!!.lastChild!!
        val s = emphasis.getSourceSpans()[0]
        assertEquals(2, s.lineIndex)
        assertEquals(4, s.columnIndex)
        assertEquals(9, s.inputIndex)
        assertEquals(5, s.length)
        assertEquals("*baz*", source.substring(s.inputIndex, s.inputIndex + s.length))
    }

    @Test
    fun addAttributes() {
        val parser = Parser.builder().build()
        val renderer =
            HtmlRenderer.builder()
                .attributeProviderFactory { ImageAttributeProvider() }
                .build()

        val document = parser.parse("![text](/url.png)")
        assertEquals("""<p><img src="/url.png" alt="text" class="border" /></p>""" + "\n", renderer.render(document))
    }

    @Test
    fun customizeRendering() {
        val parser = Parser.builder().build()
        val renderer =
            HtmlRenderer.builder()
                .nodeRendererFactory { context -> IndentedCodeBlockNodeRenderer(context) }
                .build()

        val document = parser.parse("Example:\n\n    code")
        assertEquals("<p>Example:</p>\n<pre>code\n</pre>\n", renderer.render(document))
    }

    private class WordCountVisitor : AbstractVisitor() {
        var wordCount = 0

        override fun visit(text: Text) {
            // This is called for all Text nodes. Override other visit methods for other node types.

            // Count words (this is just an example, don't actually do it this way for various reasons).
            wordCount += text.literal.orEmpty().split(Regex("""\W+""")).size

            // Descend into children (could be omitted in this case because Text nodes don't have children).
            visitChildren(text)
        }
    }

    private class ImageAttributeProvider : AttributeProvider {
        override fun setAttributes(node: Node, tagName: String, attributes: MutableMap<String, String?>) {
            if (node is Image) {
                attributes["class"] = "border"
            }
        }
    }

    private class IndentedCodeBlockNodeRenderer(context: HtmlNodeRendererContext) : NodeRenderer {
        private val html = context.writer

        // Return the node types we want to use this renderer for.
        override val nodeTypes: Set<KClass<out Node>> = setOf(IndentedCodeBlock::class)

        override fun render(node: Node) {
            // We only handle one type as per nodeTypes, so we can just cast it here.
            val codeBlock = node as IndentedCodeBlock
            html.line()
            html.tag("pre")
            html.text(codeBlock.literal.orEmpty())
            html.tag("/pre")
            html.line()
        }
    }
}
