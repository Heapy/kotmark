package io.heapy.kotmark.renderer.kotlinxhtml.internal

import io.heapy.kotmark.ext.footnotes.FootnoteDefinition
import io.heapy.kotmark.ext.footnotes.FootnoteReference
import io.heapy.kotmark.ext.footnotes.InlineFootnote
import io.heapy.kotmark.node.AbstractVisitor
import io.heapy.kotmark.node.CustomBlock
import io.heapy.kotmark.node.CustomNode
import io.heapy.kotmark.node.DefinitionMap
import io.heapy.kotmark.node.Node
import io.heapy.kotmark.node.Paragraph
import io.heapy.kotmark.renderer.NodeRenderer
import io.heapy.kotmark.renderer.kotlinxhtml.KotlinxHtmlNodeRendererContext

/** Mirrors Kotmark's footnote numbering/backlink contract using structured HTML events. */
internal class FootnoteNodeRenderer(private val context: KotlinxHtmlNodeRendererContext) : NodeRenderer {
    private val definitions = DefinitionMap(FootnoteDefinition::class)
    private val referenced = linkedMapOf<Node, ReferencedDefinition>()
    private val references = mutableMapOf<Node, ReferenceInfo>()

    override val nodeTypes = setOf(FootnoteReference::class, InlineFootnote::class, FootnoteDefinition::class)

    override fun beforeRoot(rootNode: Node) {
        rootNode.accept(object : AbstractVisitor() {
            override fun visit(customBlock: CustomBlock) {
                if (customBlock is FootnoteDefinition) definitions.putIfAbsent(customBlock.label, customBlock)
                else super.visit(customBlock)
            }
        })
    }

    override fun render(node: Node) {
        val info = when (node) {
            is FootnoteReference -> references[node] ?: definitions.get(node.label)?.let { register(it, it.label) }
            is InlineFootnote -> references[node] ?: register(node, null)
            else -> return
        }
        if (info == null) {
            context.text("[^${(node as FootnoteReference).label}]")
            return
        }
        context.element(node, "sup", mapOf("class" to "footnote-ref")) {
            context.element(node, "a", linkedMapOf("href" to "#${info.definitionId}", "id" to info.id, "data-footnote-ref" to "")) {
                context.text(info.number.toString())
            }
        }
    }

    override fun afterRoot(rootNode: Node) {
        if (referenced.isEmpty()) return
        // Discover references inside definitions before rendering backlinks. This also
        // makes repeated/self/cyclic references finite and gives breadth-first numbering.
        val queue = referenced.keys.toMutableList()
        var index = 0
        while (index < queue.size) {
            val definition = queue[index++]
            definition.accept(object : AbstractVisitor() {
                override fun visit(customNode: CustomNode) {
                    when (customNode) {
                        is FootnoteReference -> {
                            val target = definitions.get(customNode.label) ?: return
                            if (target !in referenced) queue.add(target)
                            references[customNode] = register(target, target.label)
                        }
                        is InlineFootnote -> if (customNode === definition) {
                            super.visit(customNode)
                        } else {
                            queue.add(customNode)
                            references[customNode] = register(customNode, null)
                        }
                        else -> super.visit(customNode)
                    }
                }
            })
        }
        val first = referenced.keys.first()
        context.element(first, "section", linkedMapOf("class" to "footnotes", "data-footnotes" to "")) {
            context.element(first, "ol") {
                for ([definition, info] in referenced) renderDefinition(definition, info)
            }
        }
    }

    private fun register(node: Node, label: String?): ReferenceInfo {
        val entry = referenced.getOrPut(node) {
            val number = referenced.size + 1
            ReferencedDefinition(number, if (label == null) number.toString() else "-$label")
        }
        val count = entry.references.size + 1
        val id = "fnref${entry.key}${if (count == 1) "" else "-$count"}"
        entry.references.add(id)
        return ReferenceInfo(id, "fn${entry.key}", entry.number)
    }

    private fun renderDefinition(node: Node, info: ReferencedDefinition) {
        context.element(node, "li", mapOf("id" to "fn${info.key}")) {
            val lastParagraph = node.lastChild as? Paragraph
            when {
                lastParagraph != null -> {
                    var child = node.firstChild
                    while (child != null && child !== lastParagraph) {
                        val current = child
                        child = current.next
                        if (current is Paragraph) {
                            context.element(current, "p") { context.renderChildren(current) }
                        } else context.render(current)
                    }
                    context.element(lastParagraph, "p") {
                        context.renderChildren(lastParagraph)
                        context.text(" ")
                        backrefs(node, info)
                    }
                }
                node is InlineFootnote -> context.element(node, "p") {
                    context.renderChildren(node)
                    context.text(" ")
                    backrefs(node, info)
                }
                else -> {
                    context.renderChildren(node)
                    backrefs(node, info)
                }
            }
        }
    }

    private fun backrefs(node: Node, info: ReferencedDefinition) {
        for ([index, reference] in info.references.withIndex()) {
            val count = index + 1
            val backlinkIndex = "${info.number}${if (count > 1) "-$count" else ""}"
            val attrs = linkedMapOf(
                "href" to "#$reference", "class" to "footnote-backref", "data-footnote-backref" to "",
                "data-footnote-backref-idx" to backlinkIndex, "aria-label" to "Back to reference $backlinkIndex",
            )
            context.element(node, "a", attrs) {
                if (count > 1) context.element(node, "sup", mapOf("class" to "footnote-ref")) { context.text(count.toString()) }
                context.text("\u21A9")
            }
            if (count < info.references.size) context.text(" ")
        }
    }

    private class ReferencedDefinition(val number: Int, val key: String, val references: MutableList<String> = mutableListOf())
    private class ReferenceInfo(val id: String, val definitionId: String, val number: Int)
}
