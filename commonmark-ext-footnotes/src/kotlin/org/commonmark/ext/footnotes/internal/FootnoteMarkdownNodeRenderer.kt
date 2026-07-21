package org.commonmark.ext.footnotes.internal

import org.commonmark.ext.footnotes.FootnoteDefinition
import org.commonmark.ext.footnotes.FootnoteReference
import org.commonmark.ext.footnotes.InlineFootnote
import org.commonmark.node.Node
import org.commonmark.renderer.NodeRenderer
import org.commonmark.renderer.markdown.MarkdownNodeRendererContext
import kotlin.reflect.KClass

class FootnoteMarkdownNodeRenderer(
    private val context: MarkdownNodeRendererContext,
) : NodeRenderer {
    private val writer = context.writer

    override val nodeTypes: Set<KClass<out Node>> =
        setOf(FootnoteReference::class, InlineFootnote::class, FootnoteDefinition::class)

    override fun render(node: Node) {
        when (node) {
            is FootnoteReference -> {
                writer.raw("[^")
                writer.raw(node.label)
                writer.raw("]")
            }
            is InlineFootnote -> {
                writer.raw("^[")
                renderChildren(node)
                writer.raw("]")
            }
            is FootnoteDefinition -> {
                writer.raw("[^")
                writer.raw(node.label)
                writer.raw("]: ")
                writer.pushPrefix("    ")
                renderChildren(node)
                writer.popPrefix()
            }
        }
    }

    private fun renderChildren(parent: Node) {
        var child = parent.firstChild
        while (child != null) {
            val next = child.next
            context.render(child)
            child = next
        }
    }
}
