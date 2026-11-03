package io.heapy.kotmark.renderer.text

import io.heapy.kotmark.node.Node

interface TextContentNodeRendererContext {
    val lineBreakRendering: LineBreakRendering

    @Deprecated("Use lineBreakRendering instead")
    val stripNewlines: Boolean
        get() = lineBreakRendering == LineBreakRendering.STRIP

    val writer: TextContentWriter

    fun render(node: Node)
}

/** Renders children in order, saving each next sibling before rendering a potentially mutable node. */
fun TextContentNodeRendererContext.renderChildren(parent: Node) {
    var child = parent.firstChild
    while (child != null) {
        val next = child.next
        render(child)
        child = next
    }
}
