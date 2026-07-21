package org.commonmark.ext.footnotes.internal

import org.commonmark.ext.footnotes.FootnoteDefinition
import org.commonmark.ext.footnotes.FootnoteReference
import org.commonmark.ext.footnotes.InlineFootnote
import org.commonmark.node.AbstractVisitor
import org.commonmark.node.CustomBlock
import org.commonmark.node.CustomNode
import org.commonmark.node.DefinitionMap
import org.commonmark.node.Node
import org.commonmark.node.Paragraph
import org.commonmark.renderer.NodeRenderer
import org.commonmark.renderer.html.HtmlNodeRendererContext
import kotlin.reflect.KClass

class FootnoteHtmlNodeRenderer(
    private val context: HtmlNodeRendererContext,
) : NodeRenderer {
    private val html = context.writer
    private lateinit var definitionMap: DefinitionMap<FootnoteDefinition>
    private val referencedDefinitions = linkedMapOf<Node, ReferencedDefinition>()
    private val references = mutableMapOf<Node, ReferenceInfo>()

    override val nodeTypes: Set<KClass<out Node>> =
        setOf(FootnoteReference::class, InlineFootnote::class, FootnoteDefinition::class)

    override fun beforeRoot(rootNode: Node) {
        val visitor = DefinitionVisitor()
        rootNode.accept(visitor)
        definitionMap = visitor.definitions
    }

    override fun render(node: Node) {
        when (node) {
            is FootnoteReference -> {
                val info = references[node] ?: tryRegisterReference(node)
                if (info != null) {
                    renderReference(node, info)
                } else {
                    html.text("[^${node.label}]")
                }
            }
            is InlineFootnote -> {
                val info = references[node] ?: registerReference(node, null)
                renderReference(node, info)
            }
        }
    }

    override fun afterRoot(rootNode: Node) {
        if (referencedDefinitions.isEmpty()) {
            return
        }

        val firstDefinition = referencedDefinitions.keys.first()
        html.tag(
            "section",
            context.extendAttributes(
                firstDefinition,
                "section",
                linkedMapOf("class" to "footnotes", "data-footnotes" to null),
            ),
        )
        html.line()
        html.tag("ol")
        html.line()

        val queue = referencedDefinitions.keys.toMutableList()
        var queueIndex = 0
        while (queueIndex < queue.size) {
            val definition = queue[queueIndex++]
            definition.accept(
                ShallowReferenceVisitor(definition) { reference ->
                    when (reference) {
                        is FootnoteReference -> {
                            val nestedDefinition = definitionMap.get(reference.label)
                            if (nestedDefinition != null) {
                                if (nestedDefinition !in referencedDefinitions) {
                                    queue.add(nestedDefinition)
                                }
                                references[reference] =
                                    registerReference(nestedDefinition, nestedDefinition.label)
                            }
                        }
                        is InlineFootnote -> {
                            queue.add(reference)
                            references[reference] = registerReference(reference, null)
                        }
                    }
                },
            )
        }

        for ((definition, referencedDefinition) in referencedDefinitions) {
            renderDefinition(definition, referencedDefinition)
        }
        html.tag("/ol")
        html.line()
        html.tag("/section")
        html.line()
    }

    private fun tryRegisterReference(reference: FootnoteReference): ReferenceInfo? {
        val definition = definitionMap.get(reference.label) ?: return null
        return registerReference(definition, definition.label)
    }

    private fun registerReference(node: Node, label: String?): ReferenceInfo {
        val referencedDefinition =
            referencedDefinitions.getOrPut(node) {
                val number = referencedDefinitions.size + 1
                ReferencedDefinition(number, definitionKey(label, number))
            }
        val referenceNumber = referencedDefinition.references.size + 1
        val id = referenceId(referencedDefinition.definitionKey, referenceNumber)
        referencedDefinition.references.add(id)
        return ReferenceInfo(
            id,
            definitionId(referencedDefinition.definitionKey),
            referencedDefinition.definitionNumber,
        )
    }

    private fun renderReference(node: Node, info: ReferenceInfo) {
        html.tag(
            "sup",
            context.extendAttributes(node, "sup", mapOf("class" to "footnote-ref")),
        )
        html.tag(
            "a",
            context.extendAttributes(
                node,
                "a",
                linkedMapOf(
                    "href" to "#${info.definitionId}",
                    "id" to info.id,
                    "data-footnote-ref" to null,
                ),
            ),
        )
        html.raw(info.definitionNumber.toString())
        html.tag("/a")
        html.tag("/sup")
    }

    private fun renderDefinition(
        definition: Node,
        referencedDefinition: ReferencedDefinition,
    ) {
        html.tag(
            "li",
            context.extendAttributes(
                definition,
                "li",
                mapOf("id" to definitionId(referencedDefinition.definitionKey)),
            ),
        )
        html.line()
        val lastParagraph = definition.lastChild as? Paragraph
        if (lastParagraph != null) {
            var child = definition.firstChild
            while (child != lastParagraph) {
                val current = child ?: break
                val next = current.next
                if (current is Paragraph) {
                    html.tag(
                        "p",
                        context.extendAttributes(current, "p", emptyMap()),
                    )
                    renderChildren(current)
                    html.tag("/p")
                    html.line()
                } else {
                    context.render(current)
                }
                child = next
            }
            html.tag(
                "p",
                context.extendAttributes(lastParagraph, "p", emptyMap()),
            )
            renderChildren(lastParagraph)
            html.raw(" ")
            renderBackrefs(definition, referencedDefinition)
            html.tag("/p")
            html.line()
        } else if (definition is InlineFootnote) {
            html.tag(
                "p",
                context.extendAttributes(definition, "p", emptyMap()),
            )
            renderChildren(definition)
            html.raw(" ")
            renderBackrefs(definition, referencedDefinition)
            html.tag("/p")
            html.line()
        } else {
            renderChildren(definition)
            html.line()
            renderBackrefs(definition, referencedDefinition)
        }
        html.tag("/li")
        html.line()
    }

    private fun renderBackrefs(
        definition: Node,
        referencedDefinition: ReferencedDefinition,
    ) {
        for ((index, reference) in referencedDefinition.references.withIndex()) {
            val referenceNumber = index + 1
            val backlinkIndex =
                referencedDefinition.definitionNumber.toString() +
                    if (referenceNumber > 1) "-$referenceNumber" else ""
            html.tag(
                "a",
                context.extendAttributes(
                    definition,
                    "a",
                    linkedMapOf(
                        "href" to "#$reference",
                        "class" to "footnote-backref",
                        "data-footnote-backref" to null,
                        "data-footnote-backref-idx" to backlinkIndex,
                        "aria-label" to "Back to reference $backlinkIndex",
                    ),
                ),
            )
            if (referenceNumber > 1) {
                html.tag(
                    "sup",
                    context.extendAttributes(
                        definition,
                        "sup",
                        mapOf("class" to "footnote-ref"),
                    ),
                )
                html.raw(referenceNumber.toString())
                html.tag("/sup")
            }
            html.raw("\u21A9")
            html.tag("/a")
            if (index + 1 < referencedDefinition.references.size) {
                html.raw(" ")
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

    private fun referenceId(definitionKey: String, number: Int): String =
        "fnref$definitionKey${if (number == 1) "" else "-$number"}"

    private fun definitionKey(label: String?, number: Int): String =
        if (label != null) "-$label" else number.toString()

    private fun definitionId(definitionKey: String): String = "fn$definitionKey"

    private class DefinitionVisitor : AbstractVisitor() {
        val definitions = DefinitionMap(FootnoteDefinition::class)

        override fun visit(customBlock: CustomBlock) {
            if (customBlock is FootnoteDefinition) {
                definitions.putIfAbsent(customBlock.label, customBlock)
            } else {
                super.visit(customBlock)
            }
        }
    }

    private class ShallowReferenceVisitor(
        private val parent: Node,
        private val consumer: (Node) -> Unit,
    ) : AbstractVisitor() {
        override fun visit(customNode: CustomNode) {
            when {
                customNode is FootnoteReference -> consumer(customNode)
                customNode is InlineFootnote && customNode == parent -> super.visit(customNode)
                customNode is InlineFootnote -> consumer(customNode)
                else -> super.visit(customNode)
            }
        }
    }

    private class ReferencedDefinition(
        val definitionNumber: Int,
        val definitionKey: String,
        val references: MutableList<String> = mutableListOf(),
    )

    private class ReferenceInfo(
        val id: String,
        val definitionId: String,
        val definitionNumber: Int,
    )
}
