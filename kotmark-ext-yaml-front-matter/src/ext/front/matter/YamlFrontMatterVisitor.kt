package io.heapy.kotmark.ext.front.matter

import io.heapy.kotmark.node.AbstractVisitor
import io.heapy.kotmark.node.CustomNode
import io.heapy.kotmark.node.Node
import kotlin.jvm.JvmStatic

class YamlFrontMatterVisitor : AbstractVisitor() {
    val data: MutableMap<String, List<String>> = linkedMapOf()
    var content: String = ""
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
            is YamlFrontMatterContent -> {
                content = customNode.content
                isFrontMatterPresent = true
            }
            else -> super.visit(customNode)
        }
    }

    companion object {
        /** Reads metadata produced by the default data extractor, or an empty map. */
        @JvmStatic
        fun readData(document: Node): Map<String, List<String>> =
            YamlFrontMatterVisitor().also { document.accept(it) }.data

        /** Reads text produced by the content extractor, or an empty string. */
        @JvmStatic
        fun readContent(document: Node): String =
            YamlFrontMatterVisitor().also { document.accept(it) }.content
    }
}
