package io.heapy.kotmark.internal

import io.heapy.kotmark.internal.LinkReferenceDefinitionParser.State
import io.heapy.kotmark.node.LinkReferenceDefinition
import io.heapy.kotmark.node.SourceSpan
import io.heapy.kotmark.parser.SourceLine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LinkReferenceDefinitionParserTest {
    @Test
    fun startAndInvalidLabels() {
        assertState("[", State.LABEL, "[")
        assertState("a", State.PARAGRAPH, "a")
        assertState("[]: /", State.PARAGRAPH, "[]: /")
        assertState("[ ]: /", State.PARAGRAPH, "[ ]: /")
        assertState("[foo] : /", State.PARAGRAPH, "[foo] : /")
        assertState("[foo[]:", State.PARAGRAPH, "[foo[]:")
        assertState("[foo]:", State.DESTINATION, "[foo]:")
    }

    @Test
    fun multilineLabel() {
        val parser = LinkReferenceDefinitionParser()
        parser.parseLine("[two")
        assertEquals(State.LABEL, parser.state)
        parser.parseLine("lines]:")
        assertEquals(State.DESTINATION, parser.state)
        parser.parseLine("/url")
        assertDefinition(parser.definitions.first(), "two\nlines", "/url", null)
    }

    @Test
    fun destinationAndTitle() {
        val parser = LinkReferenceDefinitionParser()
        parser.parseLine("[foo]: /url 'title'")
        assertEquals(State.START_DEFINITION, parser.state)
        assertEquals("", parser.paragraphLines.content)
        assertDefinition(parser.definitions.single(), "foo", "/url", "title")
    }

    @Test
    fun multilineTitle() {
        val parser = LinkReferenceDefinitionParser()
        parser.parseLine("[foo]: /url 'two")
        assertEquals(State.TITLE, parser.state)
        parser.parseLine("lines")
        parser.parseLine("'")
        assertEquals(State.START_DEFINITION, parser.state)
        assertDefinition(parser.definitions.single(), "foo", "/url", "two\nlines\n")
    }

    @Test
    fun invalidTitleStillKeepsDestinationReference() {
        val parser = LinkReferenceDefinitionParser()
        parser.parseLine("[foo]: /url")
        parser.parseLine(""""title" bad""")
        assertEquals(State.PARAGRAPH, parser.state)
        assertDefinition(parser.definitions.single(), "foo", "/url", null)
    }

    @Test
    fun invalidMultilineTitleKeepsParagraphSourceSpans() {
        val parser = LinkReferenceDefinitionParser()
        val definitionSpan = SourceSpan.of(0, 0, 0, 11)
        val titleSpan = SourceSpan.of(1, 0, 12, 6)
        val invalidSpan = SourceSpan.of(2, 0, 19, 4)
        parser.parseLine("[foo]: /url")
        parser.addSourceSpan(definitionSpan)
        assertEquals(State.START_TITLE, parser.state)
        parser.parseLine("(title")
        parser.addSourceSpan(titleSpan)
        assertEquals(State.TITLE, parser.state)
        parser.parseLine("foo(")
        parser.addSourceSpan(invalidSpan)
        assertEquals(State.PARAGRAPH, parser.state)

        val definition = parser.definitions.single()
        assertDefinition(definition, "foo", "/url", null)
        assertEquals(listOf(definitionSpan), definition.getSourceSpans())
        assertEquals("(title\nfoo(", parser.paragraphLines.content)
        assertEquals(listOf(titleSpan, invalidSpan), parser.paragraphSourceSpans)
    }

    @Test
    fun unclosedMultilineTitleKeepsParagraphSourceSpans() {
        val parser = LinkReferenceDefinitionParser()
        val definitionSpan = SourceSpan.of(0, 0, 0, 11)
        val paragraphSpan = SourceSpan.of(1, 0, 12, 14)
        parser.parseLine("[foo]: /url")
        parser.addSourceSpan(definitionSpan)
        parser.parseLine("(partial title")
        parser.addSourceSpan(paragraphSpan)

        val definition = parser.definitions.single()
        assertDefinition(definition, "foo", "/url", null)
        assertEquals(listOf(definitionSpan), definition.getSourceSpans())
        assertEquals("(partial title", parser.paragraphLines.content)
        assertEquals(listOf(paragraphSpan), parser.paragraphSourceSpans)
    }

    @Test
    fun multilineTitleWithTrailingGarbageKeepsParagraphSourceSpans() {
        val parser = LinkReferenceDefinitionParser()
        val definitionSpan = SourceSpan.of(0, 0, 0, 11)
        val titleSpan = SourceSpan.of(1, 0, 12, 6)
        val invalidSpan = SourceSpan.of(2, 0, 19, 18)
        parser.parseLine("[foo]: /url")
        parser.addSourceSpan(definitionSpan)
        parser.parseLine("\"title")
        parser.addSourceSpan(titleSpan)
        parser.parseLine("continued\" garbage")
        parser.addSourceSpan(invalidSpan)

        val definition = parser.definitions.single()
        assertDefinition(definition, "foo", "/url", null)
        assertEquals(listOf(definitionSpan), definition.getSourceSpans())
        assertEquals("\"title\ncontinued\" garbage", parser.paragraphLines.content)
        assertEquals(listOf(titleSpan, invalidSpan), parser.paragraphSourceSpans)
    }

    @Test
    fun definitionsMustStayAtParagraphStart() {
        val parser = LinkReferenceDefinitionParser()
        parser.parseLine("text")
        parser.parseLine("[foo]: /url")
        assertTrue(parser.definitions.isEmpty())
        assertEquals("text\n[foo]: /url", parser.paragraphLines.content)
    }

    @Test
    fun removeLinesKeepsEarlierLinesAndReturnsRemovedSpansInOrder() {
        val parser = LinkReferenceDefinitionParser()
        val lines = listOf("first", "second", "third")
        var inputIndex = 0
        val spans = lines.mapIndexed { index, content ->
            val span = SourceSpan.of(index, 0, inputIndex, content.length)
            parser.parse(SourceLine.of(content, span))
            parser.addSourceSpan(span)
            inputIndex += content.length + 1
            span
        }

        assertEquals(spans.takeLast(2), parser.removeLines(2))
        assertEquals("first", parser.paragraphLines.content)
        assertEquals(spans.take(1), parser.paragraphSourceSpans)
    }

    @Test
    fun removingAllParagraphLinesPreservesReferenceSourceSpans() {
        val parser = LinkReferenceDefinitionParser()
        val definitionSpan = SourceSpan.of(0, 0, 0, 11)
        val paragraphSpan = SourceSpan.of(1, 0, 12, 14)
        parser.parseLine("[foo]: /url")
        parser.addSourceSpan(definitionSpan)
        parser.parseLine("(partial title")
        parser.addSourceSpan(paragraphSpan)

        assertEquals(listOf(paragraphSpan), parser.removeLines(Int.MAX_VALUE))
        val definition = parser.definitions.single()
        assertDefinition(definition, "foo", "/url", null)
        assertEquals(listOf(definitionSpan), definition.getSourceSpans())
        assertTrue(parser.paragraphSourceSpans.isEmpty())
    }

    private fun LinkReferenceDefinitionParser.parseLine(content: String) {
        parse(SourceLine.of(content, null))
    }

    private fun assertState(
        input: String,
        expectedState: State,
        expectedParagraph: String,
    ) {
        val parser = LinkReferenceDefinitionParser()
        parser.parseLine(input)
        assertEquals(expectedState, parser.state)
        assertEquals(expectedParagraph, parser.paragraphLines.content)
    }

    private fun assertDefinition(
        definition: LinkReferenceDefinition,
        label: String,
        destination: String,
        title: String?,
    ) {
        assertEquals(label, definition.label)
        assertEquals(destination, definition.destination)
        assertEquals(title, definition.title)
    }
}
