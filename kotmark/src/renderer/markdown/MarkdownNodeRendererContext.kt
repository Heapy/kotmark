package io.heapy.kotmark.renderer.markdown

import io.heapy.kotmark.node.Node

interface MarkdownNodeRendererContext {
    val writer: MarkdownWriter

    fun render(node: Node)

    val specialCharacters: Set<Char>
}

/** Renders children in order, saving each next sibling before rendering a potentially mutable node. */
fun MarkdownNodeRendererContext.renderChildren(parent: Node) {
    var child = parent.firstChild
    while (child != null) {
        val next = child.next
        render(child)
        child = next
    }
}
