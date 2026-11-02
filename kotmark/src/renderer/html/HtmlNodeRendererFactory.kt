package io.heapy.kotmark.renderer.html

import io.heapy.kotmark.renderer.NodeRenderer

fun interface HtmlNodeRendererFactory {
    fun create(context: HtmlNodeRendererContext): NodeRenderer
}
