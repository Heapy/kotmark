package org.commonmark.renderer.text

import org.commonmark.node.AbstractVisitor
import org.commonmark.node.BlockQuote
import org.commonmark.node.BulletList
import org.commonmark.node.Code
import org.commonmark.node.Document
import org.commonmark.node.Emphasis
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Heading
import org.commonmark.node.HtmlBlock
import org.commonmark.node.HtmlInline
import org.commonmark.node.Image
import org.commonmark.node.IndentedCodeBlock
import org.commonmark.node.Link
import org.commonmark.node.ListItem
import org.commonmark.node.Node
import org.commonmark.node.OrderedList
import org.commonmark.node.Paragraph
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.Text
import org.commonmark.node.ThematicBreak
import org.commonmark.renderer.NodeRenderer
import kotlin.reflect.KClass

class CoreTextContentNodeRenderer(
    protected val context: TextContentNodeRendererContext,
) : AbstractVisitor(), NodeRenderer {
    private val textContent = context.writer
    private var listHolder: ListHolder? = null

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

    override fun visit(blockQuote: BlockQuote) {
        textContent.write('\u00AB')
        visitChildren(blockQuote)
        textContent.resetBlock()
        textContent.write('\u00BB')
        textContent.block()
    }

    override fun visit(bulletList: BulletList) {
        textContent.pushTight(bulletList.isTight)
        listHolder = BulletListHolder(listHolder, bulletList)
        visitChildren(bulletList)
        textContent.popTight()
        textContent.block()
        listHolder = listHolder?.parent
    }

    override fun visit(code: Code) {
        textContent.write('"')
        textContent.write(code.literal.orEmpty())
        textContent.write('"')
    }

    override fun visit(fencedCodeBlock: FencedCodeBlock) {
        writeCodeBlock(fencedCodeBlock.literal.orEmpty())
    }

    override fun visit(hardLineBreak: HardLineBreak) {
        writeLineBreak()
    }

    override fun visit(heading: Heading) {
        visitChildren(heading)
        if (stripNewlines()) {
            textContent.write(": ")
        } else {
            textContent.block()
        }
    }

    override fun visit(thematicBreak: ThematicBreak) {
        if (!stripNewlines()) {
            textContent.write("***")
        }
        textContent.block()
    }

    override fun visit(htmlInline: HtmlInline) {
        writeText(htmlInline.literal.orEmpty())
    }

    override fun visit(htmlBlock: HtmlBlock) {
        writeText(htmlBlock.literal.orEmpty())
    }

    override fun visit(image: Image) {
        writeLink(image, image.title, image.destination)
    }

    override fun visit(indentedCodeBlock: IndentedCodeBlock) {
        writeCodeBlock(indentedCodeBlock.literal.orEmpty())
    }

    override fun visit(link: Link) {
        writeLink(link, link.title, link.destination)
    }

    override fun visit(listItem: ListItem) {
        when (val holder = listHolder) {
            is OrderedListHolder -> {
                val marker = "${holder.counter}${holder.delimiter}"
                val spaces = " "
                textContent.write(marker)
                textContent.write(spaces)
                textContent.pushPrefix(" ".repeat(marker.length + spaces.length))
                visitChildren(listItem)
                textContent.block()
                textContent.popPrefix()
                holder.counter++
            }
            is BulletListHolder -> {
                if (!stripNewlines()) {
                    val marker = holder.marker
                    val spaces = " "
                    textContent.write(marker)
                    textContent.write(spaces)
                    textContent.pushPrefix(" ".repeat(marker.length + spaces.length))
                }
                visitChildren(listItem)
                textContent.block()
                if (!stripNewlines()) {
                    textContent.popPrefix()
                }
            }
            null -> Unit
        }
    }

    override fun visit(orderedList: OrderedList) {
        textContent.pushTight(orderedList.isTight)
        listHolder = OrderedListHolder(listHolder, orderedList)
        visitChildren(orderedList)
        textContent.popTight()
        textContent.block()
        listHolder = listHolder?.parent
    }

    override fun visit(paragraph: Paragraph) {
        visitChildren(paragraph)
        textContent.block()
    }

    override fun visit(softLineBreak: SoftLineBreak) {
        writeLineBreak()
    }

    override fun visit(text: Text) {
        writeText(text.literal.orEmpty())
    }

    override fun visitChildren(parent: Node) {
        var node = parent.firstChild
        while (node != null) {
            val next = node.next
            context.render(node)
            node = next
        }
    }

    private fun writeLineBreak() {
        if (stripNewlines()) {
            textContent.whitespace()
        } else {
            textContent.line()
        }
    }

    private fun writeCodeBlock(literal: String) {
        val withoutTrailingNewline = literal.removeSuffix("\n")
        if (stripNewlines()) {
            textContent.writeStripped(withoutTrailingNewline)
        } else {
            textContent.write(withoutTrailingNewline)
        }
        textContent.block()
    }

    private fun writeText(text: String) {
        if (stripNewlines()) {
            textContent.writeStripped(text)
        } else {
            textContent.write(text)
        }
    }

    private fun writeLink(
        node: Node,
        title: String?,
        destination: String?,
    ) {
        val hasChild = node.firstChild != null
        val hasTitle = title != null && title != destination
        val hasDestination = !destination.isNullOrEmpty()

        if (hasChild) {
            textContent.write('"')
            visitChildren(node)
            textContent.write('"')
            if (hasTitle || hasDestination) {
                textContent.whitespace()
                textContent.write('(')
            }
        }
        if (hasTitle) {
            textContent.write(requireNotNull(title))
            if (hasDestination) {
                textContent.colon()
                textContent.whitespace()
            }
        }
        if (hasDestination) {
            textContent.write(requireNotNull(destination))
        }
        if (hasChild && (hasTitle || hasDestination)) {
            textContent.write(')')
        }
    }

    private fun stripNewlines(): Boolean =
        context.lineBreakRendering == LineBreakRendering.STRIP

    private open class ListHolder(
        val parent: ListHolder?,
    )

    private class BulletListHolder(
        parent: ListHolder?,
        list: BulletList,
    ) : ListHolder(parent) {
        val marker = list.marker ?: "-"
    }

    private class OrderedListHolder(
        parent: ListHolder?,
        list: OrderedList,
    ) : ListHolder(parent) {
        val delimiter = list.markerDelimiter ?: "."
        var counter = list.markerStartNumber ?: 1
    }
}
