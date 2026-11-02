package io.heapy.kotmark.ext.footnotes.internal

import io.heapy.kotmark.ext.footnotes.FootnoteDefinition
import io.heapy.kotmark.ext.footnotes.FootnoteReference
import io.heapy.kotmark.ext.footnotes.InlineFootnote
import io.heapy.kotmark.node.Node
import io.heapy.kotmark.renderer.NodeRenderer
import io.heapy.kotmark.renderer.markdown.MarkdownNodeRendererContext
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
