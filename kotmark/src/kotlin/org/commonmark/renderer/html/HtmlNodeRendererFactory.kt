package org.commonmark.renderer.html

import org.commonmark.renderer.NodeRenderer

fun interface HtmlNodeRendererFactory {
    fun create(context: HtmlNodeRendererContext): NodeRenderer
}
