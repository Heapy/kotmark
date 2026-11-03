package io.heapy.kotmark.renderer

import io.heapy.kotmark.node.Code
import io.heapy.kotmark.node.CustomNode
import io.heapy.kotmark.node.Document
import io.heapy.kotmark.node.Emphasis
import io.heapy.kotmark.node.Heading
import io.heapy.kotmark.node.Node
import io.heapy.kotmark.node.Text
import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.html.HtmlRenderer
import io.heapy.kotmark.renderer.html.htmlRenderer
import io.heapy.kotmark.renderer.html.nodeRenderer
import io.heapy.kotmark.renderer.html.renderChildren
import io.heapy.kotmark.renderer.markdown.markdownRenderer
import io.heapy.kotmark.renderer.markdown.nodeRenderer
import io.heapy.kotmark.renderer.markdown.renderChildren
import io.heapy.kotmark.renderer.text.textContentRenderer
import io.heapy.kotmark.renderer.text.nodeRenderer
import io.heapy.kotmark.renderer.text.renderChildren
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TypedNodeRendererTest {
    @Test
    fun headingHandlerReceivesTypedNodeAndDelegatesChildren() {
        val renderer = htmlRenderer {
            nodeRenderer<Heading> { heading ->
                val tag = "h${heading.level}"
                writer.tag(tag, extendAttributes(heading, tag, emptyMap()))
                renderChildren(heading)
                writer.tag("/$tag")
            }
            attributeProviderFactory {
                io.heapy.kotmark.renderer.html.AttributeProvider { _, _, attributes ->
                    attributes["class"] = "chapter"
                }
            }
        }
        assertEquals("<h2 class=\"chapter\">Hello <em class=\"chapter\">world</em></h2>", renderer.render(parser().parse("## Hello *world*")))
    }

    @Test
    fun registrationOrderIsSharedWithFactoriesAndExtensions() {
        val extension = HtmlRenderer.HtmlRendererExtension { configuration ->
            configuration.nodeRenderer<Text> { writer.text("extension") }
        }
        val typedFirst = htmlRenderer {
            nodeRenderer<Text> { writer.text("typed") }
            nodeRendererFactory { context -> typedNodeRenderer<Text> { context.writer.text("factory") } }
            extensions(listOf(extension))
            nodeRenderer<Text> { writer.text("last") }
        }
        val factoryFirst = htmlRenderer {
            nodeRendererFactory { context -> typedNodeRenderer<Text> { context.writer.text("factory") } }
            nodeRenderer<Text> { writer.text("typed") }
        }
        assertEquals("typed", typedFirst.render(Text("original")))
        assertEquals("factory", factoryFirst.render(Text("original")))
        assertEquals("extension", htmlRenderer {
            extensions(listOf(extension))
            nodeRenderer<Text> { writer.text("typed") }
        }.render(Text("original")))
    }

    @Test
    fun matchingIsExactAndLowLevelAdapterChecksItsInput() {
        val renderer = htmlRenderer {
            nodeRenderer<ParentNode> { writer.text("parent") }
        }
        assertEquals("parent", renderer.render(ParentNode()))
        assertEquals("", renderer.render(ChildNode()))

        val handler = typedNodeRenderer<Text> { }
        assertEquals(setOf(Text::class), handler.nodeTypes)
        assertFailsWith<ClassCastException> { handler.render(Code("wrong node")) }
    }

    @Test
    fun configurationIsSnapshottedAndContextIsFreshOnEveryRender() {
        lateinit var configuration: HtmlRenderer.Configuration
        val renderer = htmlRenderer {
            configuration = this
            nodeRenderer<Text> { text -> writer.text(text.literal.orEmpty().uppercase()) }
        }
        configuration.nodeRenderer<Code> { writer.text("late") }
        val document = parser().parse("hello `code`")
        repeat(2) {
            assertEquals("<p>HELLO <code>code</code></p>\n", renderer.render(document))
        }
    }

    @Test
    fun typedAdapterInsideFactoryKeepsStateLocalToEachRender() {
        val renderer = htmlRenderer {
            nodeRendererFactory { context ->
                var count = 0
                typedNodeRenderer<Text> { text -> context.writer.text("${++count}:${text.literal}") }
            }
        }
        val document = Document().apply {
            appendChild(Text("a"))
            appendChild(Text("b"))
        }
        repeat(2) { assertEquals("1:a2:b", renderer.render(document)) }
    }

    @Test
    fun childTraversalPreservesNextSiblingWhenHandlerUnlinksCurrentNode() {
        fun document(): Node = Document().apply {
            appendChild(Text("a"))
            appendChild(Text("b"))
        }
        val html = htmlRenderer {
            nodeRenderer<Document> { renderChildren(it) }
            nodeRenderer<Text> { text -> writer.text(text.literal.orEmpty()); text.unlink() }
        }
        val markdown = markdownRenderer {
            nodeRenderer<Document> { renderChildren(it) }
            nodeRenderer<Text> { text -> writer.raw(text.literal.orEmpty()); text.unlink() }
        }
        val plain = textContentRenderer {
            nodeRenderer<Document> { renderChildren(it) }
            nodeRenderer<Text> { text -> writer.write(text.literal.orEmpty()); text.unlink() }
        }
        assertEquals("ab", html.render(document()))
        assertEquals("ab", markdown.render(document()))
        assertEquals("ab", plain.render(document()))
    }

    @Test
    fun markdownSpecialCharactersAreCopiedAndAffectOrdinaryText() {
        val characters = mutableSetOf('=')
        val renderer = markdownRenderer {
            nodeRenderer<Code>(specialCharacters = characters) { code ->
                writer.raw("==")
                writer.raw(code.literal.orEmpty())
                writer.raw("==")
            }
        }
        characters.clear()
        val document = Document().apply {
            appendChild(Text("="))
            appendChild(Code("x"))
        }
        assertEquals("\\===x==\n", renderer.render(document))
    }

    @Test
    fun textHandlerCanWrapTypedNodesAndRenderTheirChildren() {
        val renderer = textContentRenderer {
            nodeRenderer<Emphasis> { emphasis ->
                writer.write("[")
                renderChildren(emphasis)
                writer.write("]")
            }
        }
        assertEquals("hello [world]", renderer.render(parser().parse("hello *world*")))
    }

    private open class ParentNode : CustomNode()
    private class ChildNode : ParentNode()
}
