package io.heapy.kotmark.renderer.html

import io.heapy.kotmark.node.Node

fun interface AttributeProvider {
    fun setAttributes(
        node: Node,
        tagName: String,
        attributes: MutableMap<String, String?>,
    )
}
