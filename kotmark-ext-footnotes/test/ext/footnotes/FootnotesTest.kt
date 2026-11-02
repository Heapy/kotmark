package io.heapy.kotmark.ext.footnotes

import io.heapy.kotmark.Extension
import io.heapy.kotmark.node.Code
import io.heapy.kotmark.node.Emphasis
import io.heapy.kotmark.node.IndentedCodeBlock
import io.heapy.kotmark.node.Link
import io.heapy.kotmark.node.Node
import io.heapy.kotmark.node.Paragraph
import io.heapy.kotmark.node.SourceSpan
import io.heapy.kotmark.node.Text
import io.heapy.kotmark.parser.IncludeSourceSpans
import io.heapy.kotmark.parser.Parser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class FootnotesTest {
    private val extensions: List<Extension> = listOf(FootnotesExtension.create())
    private val parser = Parser.builder().extensions(extensions).build()

    @Test
    fun definitionSyntaxAndContents() {
        for (label in listOf("1", "a", "^", "*", "\\a", "🙂", "&0")) {
            assertEquals(label, find<FootnoteDefinition>(parser.parse("[^$label]: footnote\n")).label)
        }
        for (label in listOf("", " ", "a b", "]", "\r", "\n", "\t")) {
            assertNull(findOrNull<FootnoteDefinition>(parser.parse("[^$label]: footnote\n")))
        }
        assertEquals(
            "1",
            find<FootnoteDefinition>(parser.parse("   [^1]: footnote\n")).label,
        )
        assertNull(findOrNull<FootnoteDefinition>(parser.parse("    [^1]: footnote\n")))

        val interrupted = parser.parse("test\n[^1]: footnote\n")
        assertEquals("test", find<Paragraph>(interrupted).firstChild.let { (it as Text).literal })
        assertEquals("1", find<FootnoteDefinition>(interrupted).label)

        val code = find<IndentedCodeBlock>(parser.parse("[^1]:\n        code\n"))
        assertEquals("code\n", code.literal)

        val multipleParagraphs =
            find<FootnoteDefinition>(
                parser.parse("[^1]: footnote p1\n\n    footnote p2\n"),
            )
        assertEquals("footnote p1", (multipleParagraphs.firstChild?.firstChild as Text).literal)
        assertEquals("footnote p2", (multipleParagraphs.lastChild?.firstChild as Text).literal)
    }

    @Test
    fun referencesResolveOnlyAgainstDefinitions() {
        assertEquals(
            "foo",
            find<FootnoteReference>(
                parser.parse("Test [^foo]\n\n[^foo]: /url\n"),
            ).label,
        )
        assertNull(findOrNull<FootnoteReference>(parser.parse("Test [^foo]\n")))
        assertNull(
            findOrNull<FootnoteReference>(
                parser.parse("Test [^bar](/url)\n\n[^bar]: footnote\n"),
            ),
        )
        assertNull(
            findOrNull<FootnoteReference>(
                parser.parse("Test [^*foo*][foo]\n\n[^*foo*]: /url\n\n[foo]: /url"),
            ),
        )

        val emphasisDocument =
            parser.parse("Test *abc [^foo*] def*\n\n[^foo*]: def\n")
        val reference = find<FootnoteReference>(emphasisDocument)
        assertEquals("foo*", reference.label)
        assertIs<Emphasis>(reference.parent)
        assertEquals("abc ", (reference.previous as Text).literal)
        assertEquals(" def", (reference.next as Text).literal)

        val afterBang = parser.parse("Test![^foo]\n\n[^foo]: def\n")
        assertEquals("Test!", (afterBang.firstChild?.firstChild as Text).literal)
    }

    @Test
    fun inlineFootnotesParseFullInlineContent() {
        val extension = FootnotesExtension.builder().inlineFootnotes(true).build()
        val inlineParser = Parser.builder().extensions(listOf(extension)).build()
        val inline = find<InlineFootnote>(inlineParser.parse("Test ^[inline *footnote*]"))
        assertEquals("inline ", (inline.firstChild as Text).literal)
        assertIs<Emphasis>(inline.lastChild)
        assertNull(findOrNull<InlineFootnote>(inlineParser.parse("""Test \^[not inline footnote]""")))
        assertNull(findOrNull<InlineFootnote>(inlineParser.parse("Test ^[not inline footnote")))

        val codeFootnote = find<InlineFootnote>(inlineParser.parse("^[test `bla]`]"))
        assertEquals("bla]", (codeFootnote.lastChild as Code).literal)
        val linkedFootnote = find<InlineFootnote>(inlineParser.parse("^[with a [link](url)]"))
        assertEquals("url", (linkedFootnote.lastChild as Link).destination)
    }

    @Test
    fun sourceSpans() {
        val spansParser =
            Parser.builder()
                .extensions(extensions)
                .includeSourceSpans(IncludeSourceSpans.BLOCKS_AND_INLINES)
                .build()
        val document = spansParser.parse("Test [^foo]\n\n[^foo]: /url\n")
        assertEquals(
            listOf(SourceSpan.of(0, 5, 5, 6)),
            find<FootnoteReference>(document).getSourceSpans(),
        )
        assertEquals(
            listOf(SourceSpan.of(2, 0, 13, 12)),
            find<FootnoteDefinition>(document).getSourceSpans(),
        )
    }

    private inline fun <reified T : Node> find(parent: Node): T =
        findOrNull<T>(parent) ?: error("Could not find ${T::class.simpleName} in $parent")

    private inline fun <reified T : Node> findOrNull(parent: Node): T? =
        descendants(parent).filterIsInstance<T>().firstOrNull()

    private fun descendants(parent: Node): Sequence<Node> =
        sequence {
            var child = parent.firstChild
            while (child != null) {
                yield(child)
                yieldAll(descendants(child))
                child = child.next
            }
        }
}
