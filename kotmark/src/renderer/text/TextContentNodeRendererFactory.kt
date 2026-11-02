package io.heapy.kotmark.renderer.text

import io.heapy.kotmark.renderer.NodeRenderer

fun interface TextContentNodeRendererFactory {
    fun create(context: TextContentNodeRendererContext): NodeRenderer
}
