package io.heapy.kotmark.renderer.markdown

import io.heapy.kotmark.node.Node

interface MarkdownNodeRendererContext {
    val writer: MarkdownWriter

    fun render(node: Node)

    val specialCharacters: Set<Char>
}
