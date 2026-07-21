package org.commonmark.renderer.text

import org.commonmark.renderer.NodeRenderer

fun interface TextContentNodeRendererFactory {
    fun create(context: TextContentNodeRendererContext): NodeRenderer
}
