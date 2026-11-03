package io.heapy.kotmark.renderer.kotlinxhtml

import io.heapy.kotmark.node.Node
import io.heapy.kotmark.renderer.NodeRenderer
import io.heapy.kotmark.renderer.html.AttributeProviderContext
import io.heapy.kotmark.renderer.html.UrlSanitizer
import kotlinx.html.FlowContent
import kotlinx.html.TagConsumer

/** Per-render state. Child nodes must go through [render] to honor registered adapters. */
interface KotlinxHtmlNodeRendererContext : AttributeProviderContext {
    val consumer: TagConsumer<*>
    val escapeHtml: Boolean
    val sanitizeUrls: Boolean
    val urlSanitizer: UrlSanitizer
    val omitSingleParagraphP: Boolean
    val softbreak: String

    fun render(node: Node)
    fun renderChildren(parent: Node)
    fun text(value: String)

    /** Preserves CommonMark block boundaries when raw HTML is enabled. */
    fun line()

    /** Sends trusted markup to the consumer's unsafe-content API. */
    fun raw(value: String)

    fun resolveUrl(node: Node, destination: String): String
    fun encodeUrl(url: String): String
    fun extendAttributes(node: Node, tagName: String, attributes: Map<String, String?>): Map<String, String?>

    /** Emits a real kotlinx.html tag; attributes are extended before opening it. */
    fun element(
        node: Node,
        name: String,
        attributes: Map<String, String?> = emptyMap(),
        block: FlowContent.() -> Unit = {},
    )
}

/** Creates a fresh handler, including any document-local state, for each render call. */
fun interface KotlinxHtmlNodeRendererFactory {
    fun create(context: KotlinxHtmlNodeRendererContext): NodeRenderer
}

/** Explicitly installs handlers/providers without coupling syntax extensions to this renderer. */
fun interface KotlinxHtmlAdapter {
    fun extend(configuration: KotlinxHtmlRenderer.Configuration)
}
