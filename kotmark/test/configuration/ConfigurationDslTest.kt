package io.heapy.kotmark.configuration

import io.heapy.kotmark.Extension
import io.heapy.kotmark.extensions
import io.heapy.kotmark.node.HtmlInline
import io.heapy.kotmark.node.Heading
import io.heapy.kotmark.node.Node
import io.heapy.kotmark.node.Paragraph
import io.heapy.kotmark.node.Text
import io.heapy.kotmark.parser.IncludeSourceSpans
import io.heapy.kotmark.parser.Parser
import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.NodeRenderer
import io.heapy.kotmark.renderer.html.HtmlRenderer
import io.heapy.kotmark.renderer.html.htmlRenderer
import io.heapy.kotmark.renderer.markdown.MarkdownNodeRendererContext
import io.heapy.kotmark.renderer.markdown.MarkdownNodeRendererFactory
import io.heapy.kotmark.renderer.markdown.MarkdownRenderer
import io.heapy.kotmark.renderer.markdown.markdownRenderer
import io.heapy.kotmark.renderer.text.LineBreakRendering
import io.heapy.kotmark.renderer.text.TextContentRenderer
import io.heapy.kotmark.renderer.text.textContentRenderer
import io.heapy.kotmark.text.AsciiMatcher
import io.heapy.kotmark.text.asciiMatcher
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ConfigurationDslTest {
    @Test
    fun parserSnapshotsConfigurationAndRegistrations() {
        lateinit var configuration: Parser.Configuration
        val types = mutableSetOf(Heading::class)
        val calls = mutableListOf<String>()
        val parser = parser {
            configuration = this
            enabledBlockTypes = types
            types.clear()
            includeSourceSpans = IncludeSourceSpans.BLOCKS
            postProcessor { calls.add("first"); it }
            postProcessor { calls.add("second"); it }
        }
        configuration.enabledBlockTypes = emptySet()
        configuration.includeSourceSpans = IncludeSourceSpans.NONE
        configuration.postProcessor { calls.add("late"); it }

        val heading = assertIs<Heading>(parser.parse("# title").firstChild)
        assertTrue(heading.getSourceSpans().isNotEmpty())
        assertEquals(listOf("first", "second"), calls)
        assertIs<Heading>(parser().parse("# title").firstChild)
    }

    @Test
    fun extensionHooksApplyInDslOrderAndIgnoreUnrelatedExtensions() {
        val unrelated = object : Extension {}
        val disableHeadings = Parser.ParserExtension { it.enabledBlockTypes = emptySet() }
        val escapeHtml = HtmlRenderer.HtmlRendererExtension { it.escapeHtml = true }
        val extensions = extensions {
            add(unrelated)
            add(disableHeadings)
            add(escapeHtml)
        }
        val parser = parser { extensions(extensions) }
        assertIs<Paragraph>(parser.parse("# title").firstChild)
        assertEquals("&lt;b&gt;", htmlRenderer { extensions(extensions) }.render(HtmlInline().apply { literal = "<b>" }))
        assertEquals("<b>", htmlRenderer {
            extensions(extensions)
            this.escapeHtml = false
        }.render(HtmlInline().apply { literal = "<b>" }))
    }

    @Test
    fun htmlRendererSnapshotsPropertiesAndProviders() {
        lateinit var configuration: HtmlRenderer.Configuration
        val renderer = htmlRenderer {
            configuration = this
            softbreak = "<br />"
            escapeHtml = true
            attributeProviderFactory { { _, _, attributes -> attributes["class"] = "original" } }
        }
        configuration.softbreak = "changed"
        configuration.escapeHtml = false
        configuration.attributeProviderFactory { { _, _, attributes -> attributes["class"] = "late" } }
        val document = parser().parse("<b>one</b>\ntwo")
        assertEquals("<p class=\"original\">&lt;b&gt;one&lt;/b&gt;<br />two</p>\n", renderer.render(document))
        assertEquals("<p><b>one</b>\ntwo</p>\n", htmlRenderer().render(document))
    }

    @Test
    fun textRendererSnapshotsPropertiesAndFactories() {
        lateinit var configuration: TextContentRenderer.Configuration
        val renderer = textContentRenderer {
            configuration = this
            lineBreakRendering = LineBreakRendering.STRIP
        }
        configuration.lineBreakRendering = LineBreakRendering.SEPARATE_BLOCKS
        configuration.nodeRendererFactory { error("Late registration must not be used") }
        assertEquals("one two", renderer.render(parser().parse("one\ntwo")))
    }

    @Test
    fun markdownRendererSnapshotsFactoriesAndPreservesPrecedence() {
        lateinit var configuration: MarkdownRenderer.Configuration
        fun factory(value: String) = object : MarkdownNodeRendererFactory {
            override val specialCharacters = emptySet<Char>()
            override fun create(context: MarkdownNodeRendererContext): NodeRenderer = object : NodeRenderer {
                override val nodeTypes = setOf(Text::class)
                override fun render(node: Node) { context.writer.raw(value) }
            }
        }
        val renderer = markdownRenderer {
            configuration = this
            nodeRendererFactory(factory("first"))
            nodeRendererFactory(factory("second"))
        }
        configuration.nodeRendererFactory(factory("late"))
        assertEquals("first", renderer.render(Text("input")))
        assertEquals("input", markdownRenderer().render(Text("input")))
    }

    @Test
    fun asciiMatcherCopiesAndCapturedConfigurationsAreIndependent() {
        lateinit var configuration: AsciiMatcher.Configuration
        val original = asciiMatcher {
            configuration = this
            range('a', 'z')
        }
        val copy = asciiMatcher(original) { anyOf("01") }
        configuration.c('!')
        assertTrue(original.matches('a'))
        assertFalse(original.matches('0'))
        assertFalse(original.matches('!'))
        assertTrue(copy.matches('a'))
        assertTrue(copy.matches('0'))
        assertFalse(copy.matches('!'))
        assertFalse(copy.matches('é'))
        assertFailsWith<IllegalArgumentException> { asciiMatcher { c('é') } }
        assertFailsWith<IllegalArgumentException> { asciiMatcher { range('~', '\u0080') } }
    }
}
