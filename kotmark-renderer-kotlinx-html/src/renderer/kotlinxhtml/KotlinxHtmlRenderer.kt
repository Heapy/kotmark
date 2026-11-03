package io.heapy.kotmark.renderer.kotlinxhtml

import io.heapy.kotmark.KotmarkDsl
import io.heapy.kotmark.internal.util.Escaping
import io.heapy.kotmark.node.Node
import io.heapy.kotmark.renderer.NodeRenderer
import io.heapy.kotmark.renderer.Renderer
import io.heapy.kotmark.renderer.html.AttributeProviderFactory
import io.heapy.kotmark.renderer.html.DefaultUrlSanitizer
import io.heapy.kotmark.renderer.html.UrlResolver
import io.heapy.kotmark.renderer.html.UrlSanitizer
import io.heapy.kotmark.renderer.kotlinxhtml.internal.CoreNodeRenderer
import kotlinx.html.FlowContent
import kotlinx.html.HTMLTag
import kotlinx.html.TagConsumer
import kotlinx.html.stream.appendHTML
import kotlinx.html.visit
import kotlin.reflect.KClass

/** Renders AST nodes as kotlinx.html events. No syntax extension is loaded implicitly. */
class KotlinxHtmlRenderer private constructor(configuration: Configuration) : Renderer {
    private val escapeHtml = configuration.escapeHtml
    private val sanitizeUrls = configuration.sanitizeUrls
    private val urlSanitizer = configuration.urlSanitizer
    private val urlResolver = configuration.urlResolver
    private val percentEncodeUrls = configuration.percentEncodeUrls
    private val omitSingleParagraphP = configuration.omitSingleParagraphP
    private val softbreak = configuration.softbreak
    private val factories = configuration.factories.toList() + KotlinxHtmlNodeRendererFactory(::CoreNodeRenderer)
    private val attributeFactories = configuration.attributeFactories.toList()

    override fun render(node: Node): String = buildString { render(node, this) }

    override fun render(node: Node, output: Appendable) {
        val consumer = output.appendHTML(prettyPrint = false)
        render(node, consumer)
        consumer.finalize()
    }

    /** Inserts into an open DSL container without finalizing or adding a wrapper. */
    fun render(node: Node, into: FlowContent) = render(node, into.consumer)

    /** Emits a fragment. The caller owns the consumer and its finalization. */
    fun render(node: Node, consumer: TagConsumer<*>) {
        val context = Context(consumer)
        context.renderers.forEach { it.beforeRoot(node) }
        context.render(node)
        context.renderers.forEach { it.afterRoot(node) }
    }

    @KotmarkDsl
    class Configuration internal constructor() {
        /** Raw Markdown HTML is escaped by default, including when rendering into a DOM. */
        var escapeHtml: Boolean = true
        var sanitizeUrls: Boolean = true
        var urlSanitizer: UrlSanitizer = DefaultUrlSanitizer()
        var urlResolver: UrlResolver? = null
        var percentEncodeUrls: Boolean = false
        var omitSingleParagraphP: Boolean = false
        /** Trusted replacement markup for soft breaks. The default is an ordinary newline. */
        var softbreak: String = "\n"
        internal val factories = mutableListOf<KotlinxHtmlNodeRendererFactory>()
        internal val attributeFactories = mutableListOf<AttributeProviderFactory>()

        /** First handler registered for a node type wins; core handlers come last. */
        fun nodeRendererFactory(factory: KotlinxHtmlNodeRendererFactory) {
            factories.add(factory)
        }

        fun attributeProviderFactory(factory: AttributeProviderFactory) {
            attributeFactories.add(factory)
        }

        fun adapters(vararg adapters: KotlinxHtmlAdapter) = adapters(adapters.asIterable())

        fun adapters(adapters: Iterable<KotlinxHtmlAdapter>) {
            adapters.forEach { it.extend(this) }
        }

        internal fun create(): KotlinxHtmlRenderer = KotlinxHtmlRenderer(this)
    }

    private inner class Context(override val consumer: TagConsumer<*>) : KotlinxHtmlNodeRendererContext {
        private var lastChar = '\u0000'
        private val providers = attributeFactories.map { it.create(this) }
        val renderers = factories.map { it.create(this) }
        private val handlers = buildMap<KClass<out Node>, NodeRenderer> {
            for (renderer in renderers) {
                for (type in renderer.nodeTypes) {
                    if (type !in this) put(type, renderer)
                }
            }
        }

        override val escapeHtml get() = this@KotlinxHtmlRenderer.escapeHtml
        override val sanitizeUrls get() = this@KotlinxHtmlRenderer.sanitizeUrls
        override val urlSanitizer get() = this@KotlinxHtmlRenderer.urlSanitizer
        override val omitSingleParagraphP get() = this@KotlinxHtmlRenderer.omitSingleParagraphP
        override val softbreak get() = this@KotlinxHtmlRenderer.softbreak

        override fun render(node: Node) {
            val renderer = handlers[node::class]
                ?: error("No kotlinx.html handler for ${node::class}. Register its adapter explicitly.")
            renderer.render(node)
        }

        override fun renderChildren(parent: Node) {
            var child = parent.firstChild
            while (child != null) {
                val next = child.next
                render(child)
                child = next
            }
        }

        override fun text(value: String) {
            // kotlinx.html 0.12 treats backslash + ampersand in one content event
            // as an escape hatch. Split the events to preserve literal Markdown text.
            var start = 0
            for (index in 1 until value.length) {
                if (value[index] == '&' && value[index - 1] == '\\') {
                    consumer.onTagContent(value.substring(start, index))
                    start = index
                }
            }
            consumer.onTagContent(value.substring(start))
            if (value.isNotEmpty()) lastChar = value.last()
        }

        override fun line() {
            if (!escapeHtml && lastChar != '\u0000' && lastChar != '\n') text("\n")
        }

        override fun raw(value: String) {
            if (value.isNotEmpty()) {
                consumer.onTagContentUnsafe { +value }
                lastChar = value.last()
            }
        }

        override fun resolveUrl(node: Node, destination: String): String =
            urlResolver?.resolveUrl(node, destination) ?: destination

        override fun encodeUrl(url: String): String =
            if (percentEncodeUrls) Escaping.percentEncodeUrl(url) else url

        override fun extendAttributes(node: Node, tagName: String, attributes: Map<String, String?>): Map<String, String?> =
            attributes.toMutableMap().also { result ->
                providers.forEach { it.setAttributes(node, tagName, result) }
            }

        override fun element(node: Node, name: String, attributes: Map<String, String?>, block: FlowContent.() -> Unit) {
            require(TAG_NAME.matches(name)) { "Invalid HTML tag name: $name" }
            val attrs = extendAttributes(node, name, attributes).mapValues { it.value.orEmpty() }
            ContentTag(name, consumer, attrs).visit {
                lastChar = '>'
                block()
            }
            lastChar = '>'
        }
    }
}

private val TAG_NAME = Regex("[A-Za-z][A-Za-z0-9:-]*")
private val VOID_TAGS = setOf("area", "base", "br", "col", "embed", "hr", "img", "input", "link", "meta", "param", "source", "track", "wbr")
private val INLINE_TAGS = setOf("a", "br", "code", "del", "em", "img", "input", "ins", "mark", "span", "strong", "sub", "sup")

private class ContentTag(name: String, consumer: TagConsumer<*>, attributes: Map<String, String>) :
    HTMLTag(name, consumer, attributes, inlineTag = name in INLINE_TAGS, emptyTag = name in VOID_TAGS), FlowContent

/** Creates a renderer with a snapshot of the configured options and registrations. */
fun kotlinxHtmlRenderer(configure: KotlinxHtmlRenderer.Configuration.() -> Unit = {}): KotlinxHtmlRenderer =
    KotlinxHtmlRenderer.Configuration().apply(configure).create()
