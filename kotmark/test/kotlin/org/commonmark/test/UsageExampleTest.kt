package org.commonmark.test

import org.commonmark.node.AbstractVisitor
import org.commonmark.node.Document
import org.commonmark.node.Heading
import org.commonmark.node.Image
import org.commonmark.node.IndentedCodeBlock
import org.commonmark.node.Node
import org.commonmark.node.Text
import org.commonmark.parser.IncludeSourceSpans
import org.commonmark.parser.Parser
import org.commonmark.renderer.NodeRenderer
import org.commonmark.renderer.html.AttributeProvider
import org.commonmark.renderer.html.HtmlNodeRendererContext
import org.commonmark.renderer.html.HtmlRenderer
import org.commonmark.renderer.markdown.MarkdownRenderer
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
        assertEquals("<p><img src=\"/url.png\" alt=\"text\" class=\"border\" /></p>\n", renderer.render(document))
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
            wordCount += text.literal.orEmpty().split(Regex("\\W+")).size

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
