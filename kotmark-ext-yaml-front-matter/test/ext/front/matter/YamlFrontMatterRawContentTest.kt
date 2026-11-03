package io.heapy.kotmark.ext.front.matter

import io.heapy.kotmark.ext.front.matter.parser.FrontMatterParser
import io.heapy.kotmark.ext.front.matter.parser.FrontMatterParser.SeparatorRole
import io.heapy.kotmark.ext.front.matter.parser.RawContentParser
import io.heapy.kotmark.node.CustomNode
import io.heapy.kotmark.node.Node
import io.heapy.kotmark.parser.SourceLine
import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.html.htmlRenderer
import io.heapy.kotmark.renderer.markdown.markdownRenderer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class YamlFrontMatterRawContentTest {
    private val extension = YamlFrontMatterExtension.create(RawContentParser.Factory())
    private val parser = parser { extensions(listOf(extension)) }
    private val html = htmlRenderer { extensions(listOf(extension)) }
    private val markdown = markdownRenderer { extensions(listOf(extension)) }

    @Test
    fun preservesYamlSyntaxAndWhitespaceWithoutRenderingItAsHtml() {
        val content = "# metadata\nauthor:\n  name: 'Alice'  \n  active: true\ntags: [kotlin, markdown]\n\n"
        val document = parser.parse("---\n${content}...\n\ngreat")

        assertEquals(content, YamlFrontMatterVisitor.readRawContent(document))
        assertTrue(YamlFrontMatterVisitor.readData(document).isEmpty())
        assertEquals("<p>great</p>\n", html.render(document))
        assertEquals("---\n${content}---\n\ngreat\n", markdown.render(document))
    }

    @Test
    fun indentedDashesAreContentAndLineEndingsAreNormalized() {
        val document = parser.parse("---\r\n  first: foo\r\n  second: |\r\n    ---\r\n  third: bar\r\n...\r\n\r\ngreat")

        assertEquals("  first: foo\n  second: |\n    ---\n  third: bar\n", YamlFrontMatterVisitor.readRawContent(document))
        assertEquals("<p>great</p>\n", html.render(document))
    }

    @Test
    fun modifiedContentGetsATrailingNewlineBeforeClosingDelimiter() {
        val document = parser.parse("---\nhello: world\n---\n\ngreat")
        val content = assertIs<YamlFrontMatterRawContent>(document.firstChild?.firstChild)
        content.content = "see: you    "

        assertEquals("see: you    ", YamlFrontMatterVisitor.readRawContent(document))
        assertTrue(visit(document).isFrontMatterPresent)
        assertEquals("---\nsee: you    \n---\n\ngreat\n", markdown.render(document))

        content.content = "see: you\n"
        assertEquals("---\nsee: you\n---\n\ngreat\n", markdown.render(document))
    }

    @Test
    fun contentRoundTripsAndStateDoesNotLeakBetweenParseCalls() {
        for (content in listOf("title: My Document\n", "next: document\n")) {
            val input = "---\n${content}---\n\nMarkdown content\n"
            val document = parser.parse(input)
            assertEquals(content, YamlFrontMatterVisitor.readRawContent(document))
            assertEquals(input, markdown.render(document))
        }
        assertFalse(visit(parser.parse("great!")).isFrontMatterPresent)
        assertEquals("", YamlFrontMatterVisitor.readRawContent(parser.parse("great!")))
    }

    @Test
    fun emptyContentIsPresentButAnEmptyDataBlockHasNoMetadataNodes() {
        val contentDocument = parser.parse("---\n---\ntest")
        assertTrue(visit(contentDocument).isFrontMatterPresent)
        assertEquals("", YamlFrontMatterVisitor.readRawContent(contentDocument))

        val dataParser = parser { extensions(listOf(YamlFrontMatterExtension.create())) }
        assertFalse(visit(dataParser.parse("---\n---\ntest")).isFrontMatterPresent)
    }

    @Test
    fun defaultParserPreservesDataAccessAndHasNoRawContent() {
        val dataParser = parser { extensions(listOf(YamlFrontMatterExtension.create())) }
        val document = dataParser.parse("---\nhello: 'world'\n---\n")

        assertEquals(mapOf("hello" to listOf("world")), YamlFrontMatterVisitor.readData(document))
        assertEquals("", YamlFrontMatterVisitor.readRawContent(document))
        assertTrue(visit(document).isFrontMatterPresent)
    }

    @Test
    fun visitorDescendsThroughOtherCustomNodes() {
        val outer = object : CustomNode() {}
        outer.appendChild(YamlFrontMatterRawContent("key: value\n"))

        assertEquals("key: value\n", YamlFrontMatterVisitor.readRawContent(outer))
        assertTrue(visit(outer).isFrontMatterPresent)
    }

    @Test
    fun customParserReceivesLinesAndClosingDelimiterCallbackPerBlock() {
        val lineGroups = mutableListOf<MutableList<String>>()
        var finishedBlocks = 0
        val factory = FrontMatterParser.Factory {
            val lines = mutableListOf<String>()
            lineGroups.add(lines)
            object : FrontMatterParser {
                override fun onNextLine(block: YamlFrontMatterBlock, line: SourceLine) {
                    lines.add(line.content.toString())
                }

                override fun onEndingSeparator(block: YamlFrontMatterBlock, separator: SourceLine): SeparatorRole {
                    block.appendChild(YamlFrontMatterNode("lines", lines.toList()))
                    finishedBlocks++
                    return SeparatorRole.BLOCK_END
                }
            }
        }
        val customParser = parser { extensions(listOf(YamlFrontMatterExtension.create(factory))) }
        val first = customParser.parse("---\n  one: 1\n---\nbody")
        val second = customParser.parse("---\ntwo: 2\n...\nbody")

        assertEquals(listOf(listOf("  one: 1"), listOf("two: 2")), lineGroups)
        assertEquals(2, finishedBlocks)
        assertEquals(mapOf("lines" to listOf("  one: 1")), YamlFrontMatterVisitor.readData(first))
        assertEquals(mapOf("lines" to listOf("two: 2")), YamlFrontMatterVisitor.readData(second))
        assertEquals("<p>body</p>\n", html.render(first))

        val _ = customParser.parse("---\nunclosed: input")
        assertEquals(listOf("unclosed: input"), lineGroups.last())
        assertEquals(2, finishedBlocks, "EOF does not invoke the closing-delimiter callback")
    }

    @Test
    fun contentIsOnlyExtractedFromAClosedFrontMatterBlockAtDocumentStart() {
        for (input in listOf("---\nunclosed: input", "body\n---\nkey: value\n---", "----\nkey: value")) {
            val document = parser.parse(input)
            assertEquals("", YamlFrontMatterVisitor.readRawContent(document))
            assertFalse(visit(document).isFrontMatterPresent)
        }
    }

    private fun visit(document: Node): YamlFrontMatterVisitor =
        YamlFrontMatterVisitor().also { document.accept(it) }
}
