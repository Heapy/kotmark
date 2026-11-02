package io.heapy.kotmark.renderer.html

import io.heapy.kotmark.Extension
import io.heapy.kotmark.KotmarkDsl
import io.heapy.kotmark.internal.renderer.NodeRendererMap
import io.heapy.kotmark.internal.util.Escaping
import io.heapy.kotmark.node.Node
import io.heapy.kotmark.renderer.Renderer

class HtmlRenderer private constructor(configuration: Configuration) : Renderer {
    private val softbreak = configuration.softbreak
    private val escapeHtml = configuration.escapeHtml
    private val percentEncodeUrls = configuration.percentEncodeUrls
    private val omitSingleParagraphP = configuration.omitSingleParagraphP
    private val sanitizeUrls = configuration.sanitizeUrls
    private val urlSanitizer = configuration.urlSanitizer
    private val attributeProviderFactories = configuration.attributeProviderFactories.toList()
    private val nodeRendererFactories =
        buildList {
            addAll(configuration.nodeRendererFactories)
            add(HtmlNodeRendererFactory(::CoreHtmlNodeRenderer))
        }

    override fun render(node: Node, output: Appendable) {
        val context = RendererContext(HtmlWriter(output))
        context.beforeRoot(node)
        context.render(node)
        context.afterRoot(node)
    }

    override fun render(node: Node): String =
        buildString {
            render(node, this)
        }

    @KotmarkDsl
    class Configuration internal constructor() {
        var softbreak: String = "\n"
        var escapeHtml: Boolean = false
        var sanitizeUrls: Boolean = false
        var urlSanitizer: UrlSanitizer = DefaultUrlSanitizer()
        var percentEncodeUrls: Boolean = false
        var omitSingleParagraphP: Boolean = false
        internal val attributeProviderFactories = mutableListOf<AttributeProviderFactory>()
        internal val nodeRendererFactories = mutableListOf<HtmlNodeRendererFactory>()

        internal fun create(): HtmlRenderer = HtmlRenderer(this)

        fun attributeProviderFactory(
            attributeProviderFactory: AttributeProviderFactory,
        ) {
            attributeProviderFactories.add(attributeProviderFactory)
        }

        fun nodeRendererFactory(nodeRendererFactory: HtmlNodeRendererFactory) {
            nodeRendererFactories.add(nodeRendererFactory)
        }

        fun extensions(extensions: Iterable<Extension>) {
            for (extension in extensions) {
                if (extension is HtmlRendererExtension) {
                    extension.extend(this)
                }
            }
        }
    }

    fun interface HtmlRendererExtension : Extension {
        fun extend(rendererConfiguration: Configuration)
    }

    private inner class RendererContext(
        override val writer: HtmlWriter,
    ) : HtmlNodeRendererContext, AttributeProviderContext {
        private val attributeProviders =
            attributeProviderFactories.map { it.create(this) }
        private val nodeRendererMap =
            NodeRendererMap().also { map ->
                for (factory in nodeRendererFactories) {
                    map.add(factory.create(this))
                }
            }

        override val shouldEscapeHtml: Boolean
            get() = escapeHtml

        override val shouldOmitSingleParagraphP: Boolean
            get() = omitSingleParagraphP

        override val shouldSanitizeUrls: Boolean
            get() = sanitizeUrls

        override val urlSanitizer: UrlSanitizer
            get() = this@HtmlRenderer.urlSanitizer

        override val softbreak: String
            get() = this@HtmlRenderer.softbreak

        override fun encodeUrl(url: String): String =
            if (percentEncodeUrls) {
                Escaping.percentEncodeUrl(url)
            } else {
                url
            }

        override fun extendAttributes(
            node: Node,
            tagName: String,
            attributes: Map<String, String?>,
        ): Map<String, String?> =
            attributes.toMutableMap().also { extended ->
                for (attributeProvider in attributeProviders) {
                    attributeProvider.setAttributes(node, tagName, extended)
                }
            }

        override fun render(node: Node) {
            nodeRendererMap.render(node)
        }

        fun beforeRoot(node: Node) {
            nodeRendererMap.beforeRoot(node)
        }

        fun afterRoot(node: Node) {
            nodeRendererMap.afterRoot(node)
        }
    }
}

/** Creates [HtmlRenderer] with the supplied configuration. */
fun htmlRenderer(configure: HtmlRenderer.Configuration.() -> Unit = {}): HtmlRenderer =
    HtmlRenderer.Configuration().apply(configure).create()
