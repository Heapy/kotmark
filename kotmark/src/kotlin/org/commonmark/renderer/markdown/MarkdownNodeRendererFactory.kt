package org.commonmark.renderer.markdown

import org.commonmark.renderer.NodeRenderer

interface MarkdownNodeRendererFactory {
    fun create(context: MarkdownNodeRendererContext): NodeRenderer

    val specialCharacters: Set<Char>
}
