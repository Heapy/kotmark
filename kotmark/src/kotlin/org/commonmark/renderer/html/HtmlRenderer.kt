package org.commonmark.renderer.html

import org.commonmark.Extension
import org.commonmark.internal.renderer.NodeRendererMap
import org.commonmark.internal.util.Escaping
import org.commonmark.node.Node
import org.commonmark.renderer.Renderer
import kotlin.jvm.JvmStatic

class HtmlRenderer private constructor(builder: Builder) : Renderer {
    private val softbreak = builder.softbreak
    private val escapeHtml = builder.escapeHtml
    private val percentEncodeUrls = builder.percentEncodeUrls
    private val omitSingleParagraphP = builder.omitSingleParagraphP
    private val sanitizeUrls = builder.sanitizeUrls
    private val urlSanitizer = builder.urlSanitizer
    private val attributeProviderFactories = builder.attributeProviderFactories.toList()
    private val nodeRendererFactories =
        buildList {
            addAll(builder.nodeRendererFactories)
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

    class Builder internal constructor() {
        internal var softbreak: String = "\n"
        internal var escapeHtml: Boolean = false
        internal var sanitizeUrls: Boolean = false
        internal var urlSanitizer: UrlSanitizer = DefaultUrlSanitizer()
        internal var percentEncodeUrls: Boolean = false
        internal var omitSingleParagraphP: Boolean = false
        internal val attributeProviderFactories = mutableListOf<AttributeProviderFactory>()
        internal val nodeRendererFactories = mutableListOf<HtmlNodeRendererFactory>()

        fun build(): HtmlRenderer = HtmlRenderer(this)

        fun softbreak(softbreak: String): Builder = apply {
            this.softbreak = softbreak
        }

        fun escapeHtml(escapeHtml: Boolean): Builder = apply {
            this.escapeHtml = escapeHtml
        }

        fun sanitizeUrls(sanitizeUrls: Boolean): Builder = apply {
            this.sanitizeUrls = sanitizeUrls
        }

        fun urlSanitizer(urlSanitizer: UrlSanitizer): Builder = apply {
            this.urlSanitizer = urlSanitizer
        }

        fun percentEncodeUrls(percentEncodeUrls: Boolean): Builder = apply {
            this.percentEncodeUrls = percentEncodeUrls
        }

        fun omitSingleParagraphP(omitSingleParagraphP: Boolean): Builder = apply {
            this.omitSingleParagraphP = omitSingleParagraphP
        }

        fun attributeProviderFactory(
            attributeProviderFactory: AttributeProviderFactory,
        ): Builder = apply {
            attributeProviderFactories.add(attributeProviderFactory)
        }

        fun nodeRendererFactory(nodeRendererFactory: HtmlNodeRendererFactory): Builder = apply {
            nodeRendererFactories.add(nodeRendererFactory)
        }

        fun extensions(extensions: Iterable<Extension>): Builder = apply {
            for (extension in extensions) {
                if (extension is HtmlRendererExtension) {
                    extension.extend(this)
                }
            }
        }
    }

    fun interface HtmlRendererExtension : Extension {
        fun extend(rendererBuilder: Builder)
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

    companion object {
        @JvmStatic
        fun builder(): Builder = Builder()
    }
}
