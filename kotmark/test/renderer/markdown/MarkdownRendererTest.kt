package io.heapy.kotmark.renderer.markdown

import io.heapy.kotmark.node.BulletList
import io.heapy.kotmark.node.Document
import io.heapy.kotmark.node.Emphasis
import io.heapy.kotmark.node.FencedCodeBlock
import io.heapy.kotmark.node.Heading
import io.heapy.kotmark.node.Image
import io.heapy.kotmark.node.Link
import io.heapy.kotmark.node.ListItem
import io.heapy.kotmark.node.Node
import io.heapy.kotmark.node.OrderedList
import io.heapy.kotmark.node.Paragraph
import io.heapy.kotmark.node.Text
import io.heapy.kotmark.node.ThematicBreak
import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.NodeRenderer
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MarkdownRendererTest {
    private val destinationsWithAmpersands = listOf(
        "/url?value=&copy;",
        "/my uri?value=&copy;",
        "/url?value=&#65;",
        "/url?value=&#x41;",
        "/url?a=1&b=2",
    )
    private val titlesWithAmpersands = listOf("literal &copy;", "literal &#65;", "literal &#x41;", "bread & butter")

    @Test
    fun linkDestinationPreservesLiteralAmpersands() {
        for (destination in destinationsWithAmpersands) {
            val link = Link(destination, null).apply { appendChild(Text("link")) }
            val reparsed = assertIs<Link>(parse(render(link)).firstChild?.firstChild)
            assertEquals(destination, reparsed.destination, destination)
        }
    }

    @Test
    fun imageDestinationPreservesLiteralAmpersands() {
        for (destination in destinationsWithAmpersands) {
            val image = Image(destination, null).apply { appendChild(Text("image")) }
            val reparsed = assertIs<Image>(parse(render(image)).firstChild?.firstChild)
            assertEquals(destination, reparsed.destination, destination)
        }
    }

    @Test
    fun linkTitlePreservesLiteralAmpersands() {
        for (title in titlesWithAmpersands) {
            val link = Link("/url", title).apply { appendChild(Text("link")) }
            val reparsed = assertIs<Link>(parse(render(link)).firstChild?.firstChild)
            assertEquals(title, reparsed.title, title)
        }
    }

    @Test
    fun imageTitlePreservesLiteralAmpersands() {
        for (title in titlesWithAmpersands) {
            val image = Image("/url", title).apply { appendChild(Text("image")) }
            val reparsed = assertIs<Image>(parse(render(image)).firstChild?.firstChild)
            assertEquals(title, reparsed.title, title)
        }
    }

    @Test
    fun leafBlocksRoundTrip() {
        assertRoundTrips(
            "___\n",
            "___\n\nfoo\n",
            "* ___\n",
            "- ___\n",
            "----\n",
            "*****\n",
            "# foo\n",
            "## foo\n",
            "### foo\n",
            "#### foo\n",
            "##### foo\n",
            "###### foo\n",
            "Foo\nbar\n===\n",
            "Foo  \nbar\n===\n",
            "[foo\nbar](/url)\n===\n",
            "# foo\n\nbar\n",
            "    hi\n",
            "    hi\n    code\n",
            ">     hi\n>     code\n",
            "```\ntest\n```\n",
            "~~~~\ntest\n~~~~\n",
            "```info\ntest\n```\n",
            " ```\n test\n ```\n",
            "```\n```\n",
            "````\ntest\n````\n",
            "~~~\ntest\n~~~~~~\n",
            "<div>test</div>\n",
            """
                |> <div>
                |> test
                |> </div>
                |
            """.trimMargin(),
            "foo\n",
            "foo\n\nbar\n",
        )

        assertEquals("___", render(ThematicBreak()))

        val document = Document()
        val codeBlock = FencedCodeBlock().apply { literal = "hi code" }
        document.appendChild(codeBlock)
        assertEquals("```\nhi code\n```\n", render(document))
        codeBlock.literal = "hi`\n```\n``test"
        assertEquals("````\nhi`\n```\n``test\n````\n", render(document))
    }

    @Test
    fun containerBlocksRoundTrip() {
        assertRoundTrips(
            "> test\n",
            "> foo\n> bar\n",
            "> > foo\n> > bar\n",
            "> # Foo\n> \n> bar\n> baz\n",
            "* foo\n",
            "- foo\n",
            "+ foo\n",
            "* foo\n  bar\n",
            "* ```\n  code\n  ```\n",
            "* foo\n\n* bar\n",
            "* foo\n  \n  bar\n",
            "* foo\n* bar\n",
            "- Foo\n  - Bar\n  \n  - Baz\n",
            " -    one\n\n     two\n",
            "- \n\nFoo\n",
            "1. foo\n",
            "2. foo\n\n3. bar\n",
            "1. foo\n2. bar\n",
            "1. Foo\n   1. Bar\n   \n   2. Baz\n",
            " 1.  one\n\n    two\n",
        )

        val document = Document()
        val list = BulletList()
        val item = ListItem()
        item.appendChild(Text("Test"))
        list.appendChild(item)
        document.appendChild(list)
        assertEquals("- Test\n", render(document))
        list.marker = "*"
        assertEquals("* Test\n", render(document))

        val orderedDocument = Document()
        val ordered = OrderedList()
        val orderedItem = ListItem()
        orderedItem.appendChild(Text("Test"))
        ordered.appendChild(orderedItem)
        orderedDocument.appendChild(ordered)
        assertEquals("1. Test\n", render(orderedDocument))
        ordered.markerStartNumber = 2
        ordered.markerDelimiter = ")"
        assertEquals("2) Test\n", render(orderedDocument))

        val source = "10001.\n20.\n"
        assertEquals("10001. \n10002. \n", render(parse(source)))
    }

    @Test
    fun textEscapingRoundTrips() {
        assertRoundTrips(
            "a\tb\n",
            """\[a\](/uri)""" + "\n",
            """\`abc\`""" + "\n",
            """\- Test""" + "\n",
            """\-""" + "\n",
            "Test -\n",
            "Abc\n\n\\- Test\n",
            """\# Test""" + "\n",
            """\## Test""" + "\n",
            """\#""" + "\n",
            "Foo\n\\===\n",
            "===\n",
            "a\n\n===\n",
            """> \- Test""" + "\n",
            """- \- Test""" + "\n",
            "`a`- foo\n",
            """1\. Foo""" + "\n",
            """999\. Foo""" + "\n",
            """1\.""" + "\n",
            """1\) Foo""" + "\n",
            "&#9;foo\n",
            "&#32;   foo\n",
            "foo&#10;&#10;bar\n",
        )
    }

    @Test
    fun codeAndEmphasisRoundTrip() {
        assertRoundTrips(
            "`foo`\n",
            "``foo ` bar``\n",
            "```foo `` ` bar```\n",
            "`` `foo ``\n",
            "``  `  ``\n",
            "` `\n",
            "*foo*\n",
            "foo*bar*\n",
            "*_foo_*\n",
            "*_*foo*_*\n",
            "_*foo*_\n",
            """foo\_bar\_""" + "\n",
            "**foo**\n",
            "foo**bar**\n",
        )

        val document = Document()
        val paragraph = Paragraph()
        document.appendChild(paragraph)
        val outer = Emphasis()
        val inner = Emphasis()
        paragraph.appendChild(outer)
        outer.appendChild(inner)
        inner.appendChild(Text("hi"))
        assertEquals("*_hi_*\n", render(document))
    }

    @Test
    fun linksAndImagesRoundTrip() {
        assertRoundTrips(
            "[link](/uri)\n",
            """[link](/uri "title")""" + "\n",
            "[link](</my uri>)\n",
            "[a](<b)c>)\n",
            "[a](<b(c>)\n",
            """[a](<b\>c>)""" + "\n",
            """[a](<b\\\>c>)""" + "\n",
            """[a](/uri "foo \" bar")""" + "\n",
            """[link](/uri "tes\\")""" + "\n",
            """[link](/url "test&#10;&#10;")""" + "\n",
            "[link](</url&#10;&#10;>)\n",
            "![link](/uri)\n",
            """![link](/uri "title")""" + "\n",
            "![link](</my uri>)\n",
            "![a](<b)c>)\n",
            "![a](<b(c>)\n",
            """![a](<b\>c>)""" + "\n",
            """![a](<b\\\>c>)""" + "\n",
            """![a](/uri "foo \" bar")""" + "\n",
        )
    }

    @Test
    fun htmlAndBreaksRoundTrip() {
        assertRoundTrips(
            "<del>*foo*</del>\n",
            "foo  \nbar\n",
            "foo\nbar\n",
        )
    }

    @Test
    fun customRendererOverridesCoreRenderer() {
        val factory =
            object : MarkdownNodeRendererFactory {
                override val specialCharacters: Set<Char> = emptySet()

                override fun create(context: MarkdownNodeRendererContext): NodeRenderer =
                    object : NodeRenderer {
                        override val nodeTypes: Set<KClass<out Node>> = setOf(Heading::class)

                        override fun render(node: Node) {
                            context.writer.raw("# Custom heading")
                        }
                    }
            }
        val renderer = markdownRenderer { nodeRendererFactory(factory) }
        assertEquals("# Custom heading\n", renderer.render(parse("# Hello")))
    }

    @Test
    fun customLineSeparator() {
        val renderer = markdownRenderer { lineSeparator = "\r\n" }
        assertEquals("# Heading\r\n\r\nBody\r\n", renderer.render(parse("# Heading\n\nBody\n")))
    }

    @Test
    fun customLineSeparatorPreservesQuoteAndListPrefixes() {
        val renderer = markdownRenderer { lineSeparator = "\r\n" }
        assertEquals(
            "> # Title\r\n> \r\n> Body\r\n> \r\n> - one\r\n> - two\r\n",
            renderer.render(parse("> # Title\n> \n> Body\n> \n> - one\n> - two\n")),
        )
        assertEquals("- one\r\n\r\n- two\r\n", renderer.render(parse("- one\n\n- two\n")))
    }

    @Test
    fun lineSeparatorIsSnapshottedWhenRendererIsCreated() {
        lateinit var configuration: MarkdownRenderer.Configuration
        val renderer = markdownRenderer {
            configuration = this
            lineSeparator = "\r\n"
        }
        configuration.lineSeparator = "\r"

        val document = parse("one\ntwo\n")
        assertEquals("one\r\ntwo\r\n", renderer.render(document))
        assertEquals("one\ntwo\n", markdownRenderer().render(document))
    }

    private fun assertRoundTrips(vararg inputs: String) {
        for (input in inputs) {
            assertEquals(input, render(parse(input)), "round trip for: ${input.replace("\n", "\\n")}")
        }
    }

    private fun parse(source: String): Node = parser().parse(source)

    private fun render(node: Node): String = markdownRenderer().render(node)
}
