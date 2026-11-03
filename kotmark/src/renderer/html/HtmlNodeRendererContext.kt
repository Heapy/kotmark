package io.heapy.kotmark.renderer.html

import io.heapy.kotmark.node.Node

interface HtmlNodeRendererContext {
    /**
     * Applies the configured [UrlResolver], or returns [destination] unchanged.
     * Custom renderers should call this before sanitizing and encoding the result.
     * The default preserves destinations for existing context implementations.
     */
    fun resolveUrl(node: Node, destination: String): String = destination

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

/** Renders children in order, saving each next sibling before rendering a potentially mutable node. */
fun HtmlNodeRendererContext.renderChildren(parent: Node) {
    var child = parent.firstChild
    while (child != null) {
        val next = child.next
        render(child)
        child = next
    }
}
