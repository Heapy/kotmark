package org.commonmark.renderer.html

import org.commonmark.node.Node

interface HtmlNodeRendererContext {
    fun encodeUrl(url: String): String

    fun extendAttributes(
        node: Node,
        tagName: String,
        attributes: Map<String, String?>,
    ): Map<String, String?>

    val writer: HtmlWriter
    val softbreak: String

    fun render(node: Node)

    val shouldEscapeHtml: Boolean
    val shouldOmitSingleParagraphP: Boolean
    val shouldSanitizeUrls: Boolean
    val urlSanitizer: UrlSanitizer
}
