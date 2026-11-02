package io.heapy.kotmark.test

import io.heapy.kotmark.node.BulletList
import io.heapy.kotmark.node.Heading
import io.heapy.kotmark.node.LinkReferenceDefinition
import io.heapy.kotmark.node.ListItem
import io.heapy.kotmark.node.Node
import io.heapy.kotmark.node.Paragraph
import io.heapy.kotmark.parser.Parser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class LinkReferenceDefinitionNodeTest {
    @Test
    fun testDefinitionWithoutParagraph() {
        val document = parse("This is a paragraph with a [foo] link.\n\n[foo]: /url 'title'")
        val nodes = Nodes.getChildren(document)

        assertEquals(2, nodes.size)
        assertIs<Paragraph>(nodes[0])
        val definition = assertDef(nodes[1], "foo")

        assertEquals("/url", definition.destination)
        assertEquals("title", definition.title)
    }

    @Test
    fun testDefinitionWithParagraph() {
        val document = parse("[foo]: /url\nThis is a paragraph with a [foo] link.")
        val nodes = Nodes.getChildren(document)

        assertEquals(2, nodes.size)
        // Note that definition is not part of the paragraph, it's a sibling
        assertIs<LinkReferenceDefinition>(nodes[0])
        assertIs<Paragraph>(nodes[1])
    }

    @Test
    fun testMultipleDefinitions() {
        val document = parse("This is a paragraph with a [foo] link.\n\n[foo]: /url\n[bar]: /url")
        val nodes = Nodes.getChildren(document)

        assertEquals(3, nodes.size)
        assertIs<Paragraph>(nodes[0])
        assertDef(nodes[1], "foo")
        assertDef(nodes[2], "bar")
    }

    @Test
    fun testMultipleDefinitionsWithSameLabel() {
        val document = parse("This is a paragraph with a [foo] link.\n\n[foo]: /url1\n[foo]: /url2")
        val nodes = Nodes.getChildren(document)

        assertEquals(3, nodes.size)
        assertIs<Paragraph>(nodes[0])
        val def1 = assertDef(nodes[1], "foo")
        assertEquals("/url1", def1.destination)
        // When there's multiple definitions with the same label, the first one "wins", as in reference links will use
        // that. But we still want to preserve the original definitions in the document.
        val def2 = assertDef(nodes[2], "foo")
        assertEquals("/url2", def2.destination)
    }

    @Test
    fun testDefinitionOfReplacedBlock() {
        val document = parse("[foo]: /url\nHeading\n=======")
        val nodes = Nodes.getChildren(document)

        assertEquals(2, nodes.size)
        assertDef(nodes[0], "foo")
        assertIs<Heading>(nodes[1])
    }

    @Test
    fun testDefinitionInListItem() {
        val document = parse("* [foo]: /url\n  [foo]\n")
        assertIs<BulletList>(document.firstChild)
        val item = document.firstChild!!.firstChild
        assertIs<ListItem>(item)

        val nodes = Nodes.getChildren(item)
        assertEquals(2, nodes.size)
        assertDef(nodes[0], "foo")
        assertIs<Paragraph>(nodes[1])
    }

    @Test
    fun testDefinitionInListItem2() {
        val document = parse("* [foo]: /url\n* [foo]\n")
        assertIs<BulletList>(document.firstChild)

        val items = Nodes.getChildren(document.firstChild!!)
        assertEquals(2, items.size)
        val item1 = items[0]
        val item2 = items[1]

        assertIs<ListItem>(item1)
        assertIs<ListItem>(item2)

        assertEquals(1, Nodes.getChildren(item1).size)
        assertDef(item1.firstChild!!, "foo")

        assertEquals(1, Nodes.getChildren(item2).size)
        assertIs<Paragraph>(item2.firstChild)
    }

    @Test
    fun testDefinitionLabelCaseIsPreserved() {
        val document = parse("This is a paragraph with a [foo] link.\n\n[fOo]: /url 'title'")
        val nodes = Nodes.getChildren(document)

        assertEquals(2, nodes.size)
        assertIs<Paragraph>(nodes[0])
        assertDef(nodes[1], "fOo")
    }

    private fun parse(input: String): Node = Parser.builder().build().parse(input)

    private fun assertDef(node: Node, label: String): LinkReferenceDefinition {
        val def = assertIs<LinkReferenceDefinition>(node)
        assertEquals(label, def.label)
        return def
    }
}
