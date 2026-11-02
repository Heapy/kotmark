package io.heapy.kotmark.renderer.html

import io.heapy.kotmark.node.AbstractVisitor
import io.heapy.kotmark.node.BlockQuote
import io.heapy.kotmark.node.BulletList
import io.heapy.kotmark.node.Code
import io.heapy.kotmark.node.Document
import io.heapy.kotmark.node.Emphasis
import io.heapy.kotmark.node.FencedCodeBlock
import io.heapy.kotmark.node.HardLineBreak
import io.heapy.kotmark.node.Heading
import io.heapy.kotmark.node.HtmlBlock
import io.heapy.kotmark.node.HtmlInline
import io.heapy.kotmark.node.Image
import io.heapy.kotmark.node.IndentedCodeBlock
import io.heapy.kotmark.node.Link
import io.heapy.kotmark.node.ListBlock
import io.heapy.kotmark.node.ListItem
import io.heapy.kotmark.node.Node
import io.heapy.kotmark.node.OrderedList
import io.heapy.kotmark.node.Paragraph
import io.heapy.kotmark.node.SoftLineBreak
import io.heapy.kotmark.node.StrongEmphasis
import io.heapy.kotmark.node.Text
import io.heapy.kotmark.node.ThematicBreak
import io.heapy.kotmark.renderer.NodeRenderer
import kotlin.reflect.KClass

class CoreHtmlNodeRenderer(
    protected val context: HtmlNodeRendererContext,
) : AbstractVisitor(), NodeRenderer {
    private val html = context.writer

    override val nodeTypes: Set<KClass<out Node>> =
        setOf(
            Document::class,
            Heading::class,
            Paragraph::class,
            BlockQuote::class,
            BulletList::class,
            FencedCodeBlock::class,
            HtmlBlock::class,
            ThematicBreak::class,
            IndentedCodeBlock::class,
            Link::class,
            ListItem::class,
            OrderedList::class,
            Image::class,
            Emphasis::class,
            StrongEmphasis::class,
            Text::class,
            Code::class,
            HtmlInline::class,
            SoftLineBreak::class,
            HardLineBreak::class,
        )

    override fun render(node: Node) {
        node.accept(this)
    }

    override fun visit(document: Document) {
        visitChildren(document)
    }

    override fun visit(heading: Heading) {
        val tag = "h${heading.level}"
        html.line()
        html.tag(tag, getAttrs(heading, tag))
        visitChildren(heading)
        html.tag("/$tag")
        html.line()
    }

    override fun visit(paragraph: Paragraph) {
        val omitP =
            isInTightList(paragraph) ||
                (
                    context.shouldOmitSingleParagraphP &&
                        paragraph.parent is Document &&
                        paragraph.previous == null &&
                        paragraph.next == null
                )
        if (!omitP) {
            html.line()
            html.tag("p", getAttrs(paragraph, "p"))
        }
        visitChildren(paragraph)
        if (!omitP) {
            html.tag("/p")
            html.line()
        }
    }

    override fun visit(blockQuote: BlockQuote) {
        html.line()
        html.tag("blockquote", getAttrs(blockQuote, "blockquote"))
        html.line()
        visitChildren(blockQuote)
        html.line()
        html.tag("/blockquote")
        html.line()
    }

    override fun visit(bulletList: BulletList) {
        renderListBlock(bulletList, "ul", getAttrs(bulletList, "ul"))
    }

    override fun visit(fencedCodeBlock: FencedCodeBlock) {
        val attributes = linkedMapOf<String, String>()
        val info = fencedCodeBlock.info
        if (!info.isNullOrEmpty()) {
            attributes["class"] = "language-${info.substringBefore(' ')}"
        }
        renderCodeBlock(fencedCodeBlock.literal.orEmpty(), fencedCodeBlock, attributes)
    }

    override fun visit(htmlBlock: HtmlBlock) {
        html.line()
        if (context.shouldEscapeHtml) {
            html.tag("p", getAttrs(htmlBlock, "p"))
            html.text(htmlBlock.literal.orEmpty())
            html.tag("/p")
        } else {
            html.raw(htmlBlock.literal.orEmpty())
        }
        html.line()
    }

    override fun visit(thematicBreak: ThematicBreak) {
        html.line()
        html.tag("hr", getAttrs(thematicBreak, "hr"), true)
        html.line()
    }

    override fun visit(indentedCodeBlock: IndentedCodeBlock) {
        renderCodeBlock(indentedCodeBlock.literal.orEmpty(), indentedCodeBlock, emptyMap())
    }

    override fun visit(link: Link) {
        val attributes = linkedMapOf<String, String>()
        var url = link.destination.orEmpty()
        if (context.shouldSanitizeUrls) {
            url = context.urlSanitizer.sanitizeLinkUrl(url)
            attributes["rel"] = "nofollow"
        }
        attributes["href"] = context.encodeUrl(url)
        link.title?.let { attributes["title"] = it }
        html.tag("a", getAttrs(link, "a", attributes))
        visitChildren(link)
        html.tag("/a")
    }

    override fun visit(listItem: ListItem) {
        html.tag("li", getAttrs(listItem, "li"))
        visitChildren(listItem)
        html.tag("/li")
        html.line()
    }

    override fun visit(orderedList: OrderedList) {
        val start = orderedList.markerStartNumber ?: 1
        val attributes = linkedMapOf<String, String>()
        if (start != 1) {
            attributes["start"] = start.toString()
        }
        renderListBlock(orderedList, "ol", getAttrs(orderedList, "ol", attributes))
    }

    override fun visit(image: Image) {
        var url = image.destination.orEmpty()
        val altTextVisitor = AltTextVisitor()
        image.accept(altTextVisitor)

        val attributes = linkedMapOf<String, String>()
        if (context.shouldSanitizeUrls) {
            url = context.urlSanitizer.sanitizeImageUrl(url)
        }
        attributes["src"] = context.encodeUrl(url)
        attributes["alt"] = altTextVisitor.altText
        image.title?.let { attributes["title"] = it }
        html.tag("img", getAttrs(image, "img", attributes), true)
    }

    override fun visit(emphasis: Emphasis) {
        html.tag("em", getAttrs(emphasis, "em"))
        visitChildren(emphasis)
        html.tag("/em")
    }

    override fun visit(strongEmphasis: StrongEmphasis) {
        html.tag("strong", getAttrs(strongEmphasis, "strong"))
        visitChildren(strongEmphasis)
        html.tag("/strong")
    }

    override fun visit(text: Text) {
        html.text(text.literal.orEmpty())
    }

    override fun visit(code: Code) {
        html.tag("code", getAttrs(code, "code"))
        html.text(code.literal.orEmpty())
        html.tag("/code")
    }

    override fun visit(htmlInline: HtmlInline) {
        if (context.shouldEscapeHtml) {
            html.text(htmlInline.literal.orEmpty())
        } else {
            html.raw(htmlInline.literal.orEmpty())
        }
    }

    override fun visit(softLineBreak: SoftLineBreak) {
        html.raw(context.softbreak)
    }

    override fun visit(hardLineBreak: HardLineBreak) {
        html.tag("br", getAttrs(hardLineBreak, "br"), true)
        html.line()
    }

    override fun visitChildren(parent: Node) {
        var node = parent.firstChild
        while (node != null) {
            val next = node.next
            context.render(node)
            node = next
        }
    }

    private fun renderCodeBlock(
        literal: String,
        node: Node,
        attributes: Map<String, String?>,
    ) {
        html.line()
        html.tag("pre", getAttrs(node, "pre"))
        html.tag("code", getAttrs(node, "code", attributes))
        html.text(literal)
        html.tag("/code")
        html.tag("/pre")
        html.line()
    }

    private fun renderListBlock(
        listBlock: ListBlock,
        tagName: String,
        attributes: Map<String, String?>,
    ) {
        html.line()
        html.tag(tagName, attributes)
        html.line()
        visitChildren(listBlock)
        html.line()
        html.tag("/$tagName")
        html.line()
    }

    private fun isInTightList(paragraph: Paragraph): Boolean =
        (paragraph.parent?.parent as? ListBlock)?.isTight == true

    private fun getAttrs(
        node: Node,
        tagName: String,
        defaultAttributes: Map<String, String?> = emptyMap(),
    ): Map<String, String?> =
        context.extendAttributes(node, tagName, defaultAttributes)

    private class AltTextVisitor : AbstractVisitor() {
        private val builder = StringBuilder()
        val altText: String
            get() = builder.toString()

        override fun visit(text: Text) {
            builder.append(text.literal)
        }

        override fun visit(code: Code) {
            builder.append(code.literal)
        }

        override fun visit(softLineBreak: SoftLineBreak) {
            builder.append('\n')
        }

        override fun visit(hardLineBreak: HardLineBreak) {
            builder.append('\n')
        }
    }
}
