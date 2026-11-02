package io.heapy.kotmark.ext.autolink.internal

import io.heapy.kotmark.autolink.LinkSpan
import io.heapy.kotmark.autolink.LinkType
import io.heapy.kotmark.autolink.linkExtractor
import io.heapy.kotmark.ext.autolink.AutolinkType
import io.heapy.kotmark.node.AbstractVisitor
import io.heapy.kotmark.node.Link
import io.heapy.kotmark.node.Node
import io.heapy.kotmark.node.SourceSpan
import io.heapy.kotmark.node.Text
import io.heapy.kotmark.parser.PostProcessor

class AutolinkPostProcessor(
    linkTypes: Set<AutolinkType>,
) : PostProcessor {
    init {
        require(linkTypes.isNotEmpty()) { "linkTypes must not be empty" }
    }

    private val extractor = linkExtractor { this.linkTypes = linkTypes.map { LinkType.valueOf(it.name) }.toSet() }

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
