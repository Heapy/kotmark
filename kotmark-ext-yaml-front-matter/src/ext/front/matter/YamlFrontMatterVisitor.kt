package io.heapy.kotmark.ext.front.matter

import io.heapy.kotmark.node.AbstractVisitor
import io.heapy.kotmark.node.CustomNode
import io.heapy.kotmark.node.Node
import kotlin.jvm.JvmStatic

class YamlFrontMatterVisitor : AbstractVisitor() {
    val data: MutableMap<String, List<String>> = linkedMapOf()
    var rawContent: String = ""
        private set

    /** Whether a data or content node was visited; an empty parsed-data block has neither. */
    var isFrontMatterPresent: Boolean = false
        private set

    override fun visit(customNode: CustomNode) {
        when (customNode) {
            is YamlFrontMatterNode -> {
                data[customNode.key] = customNode.values
                isFrontMatterPresent = true
            }
            is YamlFrontMatterRawContent -> {
                rawContent = customNode.content
                isFrontMatterPresent = true
            }
            else -> super.visit(customNode)
        }
    }

    companion object {
        /** Reads metadata produced by the default YAML subset parser, or an empty map. */
        @JvmStatic
        fun readData(document: Node): Map<String, List<String>> =
            YamlFrontMatterVisitor().also { document.accept(it) }.data

        /** Reads text produced by the raw content parser, or an empty string. */
        @JvmStatic
        fun readRawContent(document: Node): String =
            YamlFrontMatterVisitor().also { document.accept(it) }.rawContent
    }
}
