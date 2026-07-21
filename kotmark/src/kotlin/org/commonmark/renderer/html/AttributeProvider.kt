package org.commonmark.renderer.html

import org.commonmark.node.Node

fun interface AttributeProvider {
    fun setAttributes(
        node: Node,
        tagName: String,
        attributes: MutableMap<String, String?>,
    )
}
