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
