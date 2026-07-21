package org.commonmark.renderer.text

import org.commonmark.node.Node

interface TextContentNodeRendererContext {
    val lineBreakRendering: LineBreakRendering

    @Deprecated("Use lineBreakRendering instead")
    val stripNewlines: Boolean
        get() = lineBreakRendering == LineBreakRendering.STRIP

    val writer: TextContentWriter

    fun render(node: Node)
}
