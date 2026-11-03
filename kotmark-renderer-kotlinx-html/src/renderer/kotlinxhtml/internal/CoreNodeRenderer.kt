package io.heapy.kotmark.renderer.kotlinxhtml.internal

import io.heapy.kotmark.node.*
import io.heapy.kotmark.renderer.NodeRenderer
import io.heapy.kotmark.renderer.kotlinxhtml.KotlinxHtmlNodeRendererContext

internal class CoreNodeRenderer(private val context: KotlinxHtmlNodeRendererContext) : NodeRenderer {
    override val nodeTypes = setOf(
        Document::class, Heading::class, Paragraph::class, BlockQuote::class, BulletList::class,
        FencedCodeBlock::class, HtmlBlock::class, ThematicBreak::class, IndentedCodeBlock::class,
        Link::class, ListItem::class, OrderedList::class, Image::class, Emphasis::class,
        StrongEmphasis::class, Text::class, Code::class, HtmlInline::class, SoftLineBreak::class,
        HardLineBreak::class, LinkReferenceDefinition::class,
    )

    override fun render(node: Node) {
        when (node) {
            is Document -> context.renderChildren(node)
            is Heading -> {
                require(node.level in 1..6) { "Heading level must be between 1 and 6" }
                blockContainer(node, "h${node.level}")
            }
            is Paragraph -> {
                val tight = (node.parent?.parent as? ListBlock)?.isTight == true
                val single = context.omitSingleParagraphP && node.parent is Document && node.previous == null && node.next == null
                if (tight || single) context.renderChildren(node) else blockContainer(node, "p")
            }
            is BlockQuote -> blockContainer(node, "blockquote", innerLines = true)
            is BulletList -> blockContainer(node, "ul", innerLines = true)
            is OrderedList -> {
                val start = node.markerStartNumber ?: 1
                blockContainer(node, "ol", if (start == 1) emptyMap() else mapOf("start" to start.toString()), innerLines = true)
            }
            is ListItem -> { container(node, "li"); context.line() }
            is FencedCodeBlock -> {
                val language = node.info.orEmpty().substringBefore(' ')
                codeBlock(node, node.literal.orEmpty(), if (language.isEmpty()) emptyMap() else mapOf("class" to "language-$language"))
            }
            is IndentedCodeBlock -> codeBlock(node, node.literal.orEmpty(), emptyMap())
            is ThematicBreak -> { context.line(); context.element(node, "hr"); context.line() }
            is Emphasis -> container(node, "em")
            is StrongEmphasis -> container(node, "strong")
            is Text -> context.text(node.literal.orEmpty())
            is Code -> context.element(node, "code") { context.text(node.literal.orEmpty()) }
            is Link -> {
                var url = context.resolveUrl(node, node.destination.orEmpty())
                val attrs = linkedMapOf<String, String?>()
                if (context.sanitizeUrls) {
                    url = context.urlSanitizer.sanitizeLinkUrl(url)
                    attrs["rel"] = "nofollow"
                }
                attrs["href"] = context.encodeUrl(url)
                node.title?.let { attrs["title"] = it }
                container(node, "a", attrs)
            }
            is Image -> {
                var url = context.resolveUrl(node, node.destination.orEmpty())
                if (context.sanitizeUrls) url = context.urlSanitizer.sanitizeImageUrl(url)
                val attrs = linkedMapOf("src" to context.encodeUrl(url), "alt" to altText(node))
                node.title?.let { attrs["title"] = it }
                context.element(node, "img", attrs)
            }
            is HtmlInline -> if (context.escapeHtml) context.text(node.literal.orEmpty()) else context.raw(node.literal.orEmpty())
            is HtmlBlock -> {
                context.line()
                if (context.escapeHtml) context.element(node, "p") { context.text(node.literal.orEmpty()) }
                else context.raw(node.literal.orEmpty())
                context.line()
            }
            is SoftLineBreak -> if (context.softbreak == "\n") context.text("\n") else context.raw(context.softbreak)
            is HardLineBreak -> { context.element(node, "br"); context.text("\n") }
            is LinkReferenceDefinition -> Unit
        }
    }

    private fun container(node: Node, name: String, attributes: Map<String, String?> = emptyMap()) {
        context.element(node, name, attributes) { context.renderChildren(node) }
    }

    private fun codeBlock(node: Node, literal: String, attributes: Map<String, String?>) {
        context.line()
        context.element(node, "pre") {
            context.element(node, "code", attributes) { context.text(literal) }
        }
        context.line()
    }

    private fun blockContainer(node: Node, name: String, attributes: Map<String, String?> = emptyMap(), innerLines: Boolean = false) {
        context.line()
        context.element(node, name, attributes) {
            if (innerLines) context.line()
            context.renderChildren(node)
            if (innerLines) context.line()
        }
        context.line()
    }

    private fun altText(node: Node): String = buildString {
        node.accept(object : AbstractVisitor() {
            override fun visit(text: Text) { append(text.literal.orEmpty()) }
            override fun visit(code: Code) { append(code.literal.orEmpty()) }
            override fun visit(softLineBreak: SoftLineBreak) { append('\n') }
            override fun visit(hardLineBreak: HardLineBreak) { append('\n') }
        })
    }
}
