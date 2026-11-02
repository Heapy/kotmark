package io.heapy.kotmark.renderer.markdown

import io.heapy.kotmark.renderer.NodeRenderer

interface MarkdownNodeRendererFactory {
    fun create(context: MarkdownNodeRendererContext): NodeRenderer

    val specialCharacters: Set<Char>
}
