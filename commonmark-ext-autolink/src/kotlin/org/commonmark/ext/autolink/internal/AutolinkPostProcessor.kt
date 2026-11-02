package org.commonmark.ext.autolink.internal

import org.commonmark.ext.autolink.AutolinkType
import org.commonmark.node.AbstractVisitor
import org.commonmark.node.Link
import org.commonmark.node.Node
import org.commonmark.node.SourceSpan
import org.commonmark.node.Text
import org.commonmark.parser.PostProcessor
import org.nibor.autolink.LinkExtractor
import org.nibor.autolink.LinkType
import org.nibor.autolink.LinkSpan

class AutolinkPostProcessor(
    linkTypes: Set<AutolinkType>,
) : PostProcessor {
    init {
        require(linkTypes.isNotEmpty()) { "linkTypes must not be empty" }
    }

    private val extractor = LinkExtractor.builder()
        .linkTypes(linkTypes.map { LinkType.valueOf(it.name) }.toSet())
        .build()

    override fun process(node: Node): Node {
        node.accept(AutolinkVisitor())
        return node
    }

    private fun linkify(originalTextNode: Text) {
        val literal = originalTextNode.literal.orEmpty()
        val spans = extractor.extractSpans(literal).iterator()
        val sourceSpan = originalTextNode.getSourceSpans().singleOrNull()
        var lastNode: Node = originalTextNode
        while (spans.hasNext()) {
            val span = spans.next()
            if (lastNode === originalTextNode && !spans.hasNext() && span !is LinkSpan) return
            val textNode = createTextNode(literal, span.beginIndex, span.endIndex, sourceSpan)
            val replacement = if (span is LinkSpan) {
                val destination = when (span.type) {
                    LinkType.EMAIL -> "mailto:${textNode.literal}"
                    LinkType.WWW -> "http://${textNode.literal}"
                    LinkType.URL -> textNode.literal.orEmpty()
                }
                Link(destination, null).also { link ->
                    link.appendChild(textNode)
                    link.setSourceSpans(textNode.getSourceSpans())
                }
            } else {
                textNode
            }
            lastNode = insertNode(replacement, lastNode)
        }
        originalTextNode.unlink()
    }

    private fun createTextNode(
        literal: String,
        beginIndex: Int,
        endIndex: Int,
        sourceSpan: SourceSpan?,
    ): Text =
        Text(literal.substring(beginIndex, endIndex)).also { text ->
            sourceSpan?.let { text.addSourceSpan(it.subSpan(beginIndex, endIndex)) }
        }

    private fun insertNode(node: Node, insertAfterNode: Node): Node {
        insertAfterNode.insertAfter(node)
        return node
    }

    private inner class AutolinkVisitor : AbstractVisitor() {
        private var inLink = 0

        override fun visit(link: Link) {
            inLink++
            super.visit(link)
            inLink--
        }

        override fun visit(text: Text) {
            if (inLink == 0) {
                linkify(text)
            }
        }
    }
}
