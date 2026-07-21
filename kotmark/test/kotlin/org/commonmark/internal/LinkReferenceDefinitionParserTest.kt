package org.commonmark.internal

import org.commonmark.internal.LinkReferenceDefinitionParser.State
import org.commonmark.node.LinkReferenceDefinition
import org.commonmark.parser.SourceLine
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
        parser.parseLine("\"title\" bad")
        assertEquals(State.PARAGRAPH, parser.state)
        assertDefinition(parser.definitions.single(), "foo", "/url", null)
    }

    @Test
    fun definitionsMustStayAtParagraphStart() {
        val parser = LinkReferenceDefinitionParser()
        parser.parseLine("text")
        parser.parseLine("[foo]: /url")
        assertTrue(parser.definitions.isEmpty())
        assertEquals("text\n[foo]: /url", parser.paragraphLines.content)
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
