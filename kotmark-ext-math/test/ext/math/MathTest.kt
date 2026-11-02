package io.heapy.kotmark.ext.math

import io.heapy.kotmark.node.Node
import io.heapy.kotmark.node.SourceSpan
import io.heapy.kotmark.node.Text
import io.heapy.kotmark.parser.IncludeSourceSpans
import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.NodeRenderer
import io.heapy.kotmark.renderer.html.htmlRenderer
import io.heapy.kotmark.renderer.markdown.markdownRenderer
import io.heapy.kotmark.renderer.text.textContentRenderer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class MathTest {
    private val extensions = listOf(MathExtension.create())
    private val parser = parser { extensions(extensions) }
    private val html = htmlRenderer { extensions(extensions) }
    private val markdown = markdownRenderer { extensions(extensions) }
    private val text = textContentRenderer { extensions(extensions) }

    @Test
    fun inlineContentIsOpaqueAndEscapedOnlyForHtml() {
        val formula = $$"""\sqrt{\$4} + *x* &amp; <tag> `code` [a](b)"""
        val document = parser.parse("before $" + formula + "$ after")
        val math = assertIs<InlineMath>(document.firstChild?.firstChild?.next)
        assertEquals(formula, math.literal)
        assertNull(math.firstChild)
        assertEquals("$", math.openingDelimiter)
        assertEquals("$", math.closingDelimiter)
        assertEquals(
            $$"""<p>before <span class="math math-inline">\sqrt{\$4} + *x* &amp;amp; &lt;tag&gt; `code` [a](b)</span> after</p>""" + "\n",
            html.render(document),
        )
        assertEquals("before " + formula + " after", text.render(document))
        assertEquals("before $" + formula + "$ after\n", markdown.render(document))
    }

    @Test
    fun dollarGrammarAndCurrency() {
        for (source in listOf(
            "$\$x$", "$\$x$$", "$$\$x$$$", "$ x$", "\$x $", "\$x$2", "$", "\$x", "x$",
            "$5 and $10", "US$20", "\$x\ny$", "\$x$$", "\$x $ y$",
        )) {
            assertEquals("<p>$source</p>\n", html.render(parser.parse(source)), source)
        }
        assertEquals("<p>\$x$</p>\n", html.render(parser.parse("\\\$x\\$")))
        assertEquals("<p><code>\$x$</code></p>\n", html.render(parser.parse("`\$x$`")))
        assertEquals("<pre><code>\$x$\n</code></pre>\n", html.render(parser.parse("```\n\$x$\n```")))
        assertEquals("<p>a<span class=\"math math-inline\">x</span>b</p>\n", html.render(parser.parse("a\$x\$b")))
        assertEquals("<p><span class=\"math math-inline\">2</span></p>\n", html.render(parser.parse("$2$")))
        assertEquals(
            "<p><a href=\"/\$url$\"><span class=\"math math-inline\">x</span></a></p>\n",
            html.render(parser.parse("[\$x$](/\$url$)")),
        )
        assertIs<Text>(io.heapy.kotmark.parser.parser().parse("\$x$").firstChild?.firstChild)
        assertEquals("\uD800\uDC20", assertIs<InlineMath>(parser.parse("$\uD800\uDC20$").firstChild?.firstChild).literal)
    }

    @Test
    fun escapingUsesBackslashParityAndPreservesBackslashes() {
        val odd = $$"""$x\$y$"""
        val even = $$"""$x\\$"""
        assertEquals($$"""x\$y""", assertIs<InlineMath>(parser.parse(odd).firstChild?.firstChild).literal)
        assertEquals("""x\\""", assertIs<InlineMath>(parser.parse(even).firstChild?.firstChild).literal)
        assertEquals(odd + "\n", markdown.render(parser.parse(odd)))
        assertEquals(even + "\n", markdown.render(parser.parse(even)))
    }

    @Test
    fun blockFencesContentAndTermination() {
        val source = "$$\n  x & <y>\n\n\\frac{a}{b}\n$$\nafter"
        val document = parser.parse(source)
        val math = assertIs<MathBlock>(document.firstChild)
        assertEquals("  x & <y>\n\n\\frac{a}{b}\n", math.literal)
        assertNull(math.firstChild)
        assertEquals(
            "<div class=\"math math-block\">  x &amp; &lt;y&gt;\n\n\\frac{a}{b}\n</div>\n<p>after</p>\n",
            html.render(document),
        )
        assertEquals("x\n", assertIs<MathBlock>(parser.parse("$$\nx").firstChild).literal)
        assertEquals("", assertIs<MathBlock>(parser.parse("$$\n$$").firstChild).literal)
        assertEquals("", assertIs<MathBlock>(parser.parse("$$").firstChild).literal)
        assertEquals("x\n", assertIs<MathBlock>(parser.parse("   $$ \t\n   x\n $$\t").firstChild).literal)
        assertEquals("x\n", assertIs<MathBlock>(parser.parse("$$\r\nx\r\n$$").firstChild).literal)
        assertEquals("a\n$$$\nb\n", assertIs<MathBlock>(parser.parse("$$\na\n$$$\nb\n$$").firstChild).literal)
        assertEquals("<pre><code>$$\nx\n$$\n</code></pre>\n", html.render(parser.parse("    $$\n    x\n    $$")))
        assertEquals("<p>before</p>\n<div class=\"math math-block\">x\n</div>\n", html.render(parser.parse("before\n$$\nx\n$$")))
    }

    @Test
    fun markdownRoundTripsIncludingContainersAndLiteralDollars() {
        for (source in listOf(
            "\$x$ and \$y$\n", "$$\nx\n\ny\n$$\n", "$$\n$$\n",
            "> $$\n> x\n> $$\n", "- $$\n  x\n  $$\n",
            "cost $5 and $10\n", "$\$x$$\n", "\\\$x\\$\n", "$$\nx",
            "   $$\n    $$\n   $$\n", ">   $$\n>     $$\n>   $$\n",
        )) {
            val first = parser.parse(source)
            val rendered = markdown.render(first)
            assertEquals(html.render(first), html.render(parser.parse(rendered)), "$source -> $rendered")
        }
        assertEquals("before\nx\nafter", text.render(parser.parse("before\n\n$$\nx\n$$\n\nafter")))
        val stripped = textContentRenderer {
            extensions(extensions)
            lineBreakRendering = io.heapy.kotmark.renderer.text.LineBreakRendering.STRIP
        }
        assertEquals("before x y after", stripped.render(parser.parse("before\n\n$$\nx\ny\n$$\n\nafter")))
    }

    @Test
    fun sourceSpansIncludeDelimiters() {
        val parser = parser {
            extensions(extensions)
            includeSourceSpans = IncludeSourceSpans.BLOCKS_AND_INLINES
        }
        val inline = assertIs<InlineMath>(parser.parse("hey \$x$!").firstChild?.firstChild?.next)
        assertEquals(listOf(SourceSpan.of(0, 4, 4, 3)), inline.getSourceSpans())
        val block = assertIs<MathBlock>(parser.parse("$$\nx\n$$").firstChild)
        assertEquals(
            listOf(SourceSpan.of(0, 0, 0, 2), SourceSpan.of(1, 0, 3, 1), SourceSpan.of(2, 0, 5, 2)),
            block.getSourceSpans(),
        )
    }

    @Test
    fun integrationsCanReplaceTheHtmlRenderer() {
        val renderer = htmlRenderer {
            nodeRendererFactory { context ->
                object : NodeRenderer {
                    override val nodeTypes = setOf(InlineMath::class, MathBlock::class)

                    override fun render(node: Node) {
                        context.writer.tag("code")
                        context.writer.text(when (node) {
                            is InlineMath -> node.literal
                            is MathBlock -> node.literal
                            else -> error("Unexpected node")
                        })
                        context.writer.tag("/code")
                    }
                }
            }
            extensions(extensions)
        }
        assertEquals("<p><code>x</code></p>\n<code>y\n</code>", renderer.render(parser.parse("\$x$\n\n$$\ny\n$$")))
    }

    @Test
    fun attributeProvidersApplyToBothNodeTypes() {
        val renderer = htmlRenderer {
            extensions(extensions)
            attributeProviderFactory {
                io.heapy.kotmark.renderer.html.AttributeProvider { node, _, attributes ->
                    if (node is InlineMath || node is MathBlock) attributes["data-math"] = "true"
                }
            }
        }
        assertEquals("<p><span class=\"math math-inline\" data-math=\"true\">x</span></p>\n", renderer.render(parser.parse("\$x$")))
        assertEquals("<div class=\"math math-block\" data-math=\"true\">x\n</div>\n", renderer.render(parser.parse("$$\nx\n$$")))
    }
}
