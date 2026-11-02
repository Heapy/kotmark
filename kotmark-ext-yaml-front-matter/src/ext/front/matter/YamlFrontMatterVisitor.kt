package io.heapy.kotmark.ext.front.matter

import io.heapy.kotmark.node.AbstractVisitor
import io.heapy.kotmark.node.CustomNode

class YamlFrontMatterVisitor : AbstractVisitor() {
    val data: MutableMap<String, List<String>> = linkedMapOf()

    override fun visit(customNode: CustomNode) {
        if (customNode is YamlFrontMatterNode) {
            data[customNode.key] = customNode.values
        } else {
            super.visit(customNode)
        }
    }
}
