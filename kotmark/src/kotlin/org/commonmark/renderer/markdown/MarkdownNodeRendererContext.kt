package org.commonmark.renderer.markdown

import org.commonmark.node.Node

interface MarkdownNodeRendererContext {
    val writer: MarkdownWriter

    fun render(node: Node)

    val specialCharacters: Set<Char>
}
