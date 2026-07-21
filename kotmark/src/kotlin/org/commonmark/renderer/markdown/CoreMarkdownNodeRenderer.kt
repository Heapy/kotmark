package org.commonmark.renderer.markdown

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
import org.commonmark.text.AsciiMatcher
import org.commonmark.text.CharMatcher
import org.commonmark.text.Characters
import kotlin.reflect.KClass

class CoreMarkdownNodeRenderer(
    protected val context: MarkdownNodeRendererContext,
) : AbstractVisitor(), NodeRenderer {
    private val writer = context.writer
    private val textEscape =
        AsciiMatcher.builder()
            .anyOf("[]<>`*_&\n\\")
            .anyOf(context.specialCharacters)
            .build()
    private val textEscapeInHeading =
        textEscape.newBuilder().c('#').build()
    private val linkDestinationNeedsAngleBrackets =
        AsciiMatcher.builder().anyOf(" ()<>\n\\").build()
    private val linkDestinationEscapeInAngleBrackets =
        AsciiMatcher.builder().anyOf("<>\n\\").build()
    private val linkTitleEscapeInQuotes =
        AsciiMatcher.builder().anyOf("\"\n\\").build()
    private val orderedListMarkerPattern = Regex("^([0-9]{1,9})([.)])")
    private var listHolder: ListHolder? = null

    override val nodeTypes: Set<KClass<out Node>> =
        setOf(
            BlockQuote::class,
            BulletList::class,
            Code::class,
            Document::class,
            Emphasis::class,
            FencedCodeBlock::class,
            HardLineBreak::class,
            Heading::class,
            HtmlBlock::class,
            HtmlInline::class,
            Image::class,
            IndentedCodeBlock::class,
            Link::class,
            ListItem::class,
            OrderedList::class,
            Paragraph::class,
            SoftLineBreak::class,
            StrongEmphasis::class,
            Text::class,
            ThematicBreak::class,
        )

    override fun render(node: Node) {
        node.accept(this)
    }

    override fun visit(document: Document) {
        visitChildren(document)
        writer.line()
    }

    override fun visit(thematicBreak: ThematicBreak) {
        writer.raw(thematicBreak.literal ?: "___")
        writer.block()
    }

    override fun visit(heading: Heading) {
        if (heading.level <= 2) {
            val lineBreakVisitor = LineBreakVisitor()
            heading.accept(lineBreakVisitor)
            if (lineBreakVisitor.hasLineBreak) {
                visitChildren(heading)
                writer.line()
                writer.raw(if (heading.level == 1) "===" else "---")
                writer.block()
                return
            }
        }

        repeat(heading.level) {
            writer.raw('#')
        }
        writer.raw(' ')
        visitChildren(heading)
        writer.block()
    }

    override fun visit(indentedCodeBlock: IndentedCodeBlock) {
        writer.writePrefix("    ")
        writer.pushPrefix("    ")
        val lines = getLines(indentedCodeBlock.literal.orEmpty())
        for ((index, line) in lines.withIndex()) {
            writer.raw(line)
            if (index != lines.lastIndex) {
                writer.line()
            }
        }
        writer.popPrefix()
        writer.block()
    }

    override fun visit(fencedCodeBlock: FencedCodeBlock) {
        val literal = fencedCodeBlock.literal.orEmpty()
        val fenceCharacter = fencedCodeBlock.fenceCharacter ?: "`"
        val openingFenceLength =
            fencedCodeBlock.openingFenceLength
                ?: maxOf(findMaxRunLength(fenceCharacter, literal) + 1, 3)
        val closingFenceLength = fencedCodeBlock.closingFenceLength ?: openingFenceLength
        val openingFence = fenceCharacter.repeat(openingFenceLength)
        val closingFence = fenceCharacter.repeat(closingFenceLength)
        val indent = fencedCodeBlock.fenceIndent

        if (indent > 0) {
            val indentPrefix = " ".repeat(indent)
            writer.writePrefix(indentPrefix)
            writer.pushPrefix(indentPrefix)
        }

        writer.raw(openingFence)
        fencedCodeBlock.info?.let(writer::raw)
        writer.line()
        if (literal.isNotEmpty()) {
            for (line in getLines(literal)) {
                writer.raw(line)
                writer.line()
            }
        }
        writer.raw(closingFence)
        if (indent > 0) {
            writer.popPrefix()
        }
        writer.block()
    }

    override fun visit(htmlBlock: HtmlBlock) {
        val lines = getLines(htmlBlock.literal.orEmpty())
        for ((index, line) in lines.withIndex()) {
            writer.raw(line)
            if (index != lines.lastIndex) {
                writer.line()
            }
        }
        writer.block()
    }

    override fun visit(paragraph: Paragraph) {
        visitChildren(paragraph)
        writer.block()
    }

    override fun visit(blockQuote: BlockQuote) {
        writer.writePrefix("> ")
        writer.pushPrefix("> ")
        visitChildren(blockQuote)
        writer.popPrefix()
        writer.block()
    }

    override fun visit(bulletList: BulletList) {
        writer.pushTight(bulletList.isTight)
        listHolder = BulletListHolder(listHolder, bulletList)
        visitChildren(bulletList)
        listHolder = listHolder?.parent
        writer.popTight()
        writer.block()
    }

    override fun visit(orderedList: OrderedList) {
        writer.pushTight(orderedList.isTight)
        listHolder = OrderedListHolder(listHolder, orderedList)
        visitChildren(orderedList)
        listHolder = listHolder?.parent
        writer.popTight()
        writer.block()
    }

    override fun visit(listItem: ListItem) {
        val markerIndent = listItem.markerIndent ?: 0
        val holder = listHolder
        val marker =
            when (holder) {
                is BulletListHolder -> " ".repeat(markerIndent) + holder.marker
                is OrderedListHolder -> {
                    val value = " ".repeat(markerIndent) + holder.number + holder.delimiter
                    holder.number++
                    value
                }
                else -> error("Unknown list holder type: $holder")
            }
        val spaces =
            listItem.contentIndent?.let {
                " ".repeat(maxOf(it - marker.length, 1))
            } ?: " "
        writer.writePrefix(marker)
        writer.writePrefix(spaces)
        writer.pushPrefix(" ".repeat(marker.length + spaces.length))

        if (listItem.firstChild == null) {
            writer.block()
        } else {
            visitChildren(listItem)
        }
        writer.popPrefix()
    }

    override fun visit(code: Code) {
        val literal = code.literal.orEmpty()
        val backticks = findMaxRunLength("`", literal)
        repeat(backticks + 1) {
            writer.raw('`')
        }
        val addSpace =
            literal.startsWith('`') ||
                literal.endsWith('`') ||
                (
                    literal.startsWith(' ') &&
                        literal.endsWith(' ') &&
                        Characters.hasNonSpace(literal)
                )
        if (addSpace) {
            writer.raw(' ')
        }
        writer.raw(literal)
        if (addSpace) {
            writer.raw(' ')
        }
        repeat(backticks + 1) {
            writer.raw('`')
        }
    }

    override fun visit(emphasis: Emphasis) {
        val delimiter =
            emphasis.openingDelimiter
                ?: if (writer.lastChar == '*') "_" else "*"
        writer.raw(delimiter)
        super.visit(emphasis)
        writer.raw(delimiter)
    }

    override fun visit(strongEmphasis: StrongEmphasis) {
        writer.raw("**")
        super.visit(strongEmphasis)
        writer.raw("**")
    }

    override fun visit(link: Link) {
        writeLinkLike(link.title, link.destination.orEmpty(), link, "[")
    }

    override fun visit(image: Image) {
        writeLinkLike(image.title, image.destination.orEmpty(), image, "![")
    }

    override fun visit(htmlInline: HtmlInline) {
        writer.raw(htmlInline.literal.orEmpty())
    }

    override fun visit(hardLineBreak: HardLineBreak) {
        writer.raw("  ")
        writer.line()
    }

    override fun visit(softLineBreak: SoftLineBreak) {
        writer.line()
    }

    override fun visit(text: Text) {
        var literal = text.literal.orEmpty()
        if (writer.isAtLineStart && literal.isNotEmpty()) {
            when (literal.first()) {
                '-' -> {
                    writer.raw("\\-")
                    literal = literal.substring(1)
                }
                '#' -> {
                    writer.raw("\\#")
                    literal = literal.substring(1)
                }
                '=' -> {
                    if (text.previous != null) {
                        writer.raw("\\=")
                        literal = literal.substring(1)
                    }
                }
                in '0'..'9' -> {
                    orderedListMarkerPattern.find(literal)?.let { match ->
                        writer.raw(match.groupValues[1])
                        writer.raw("\\${match.groupValues[2]}")
                        literal = literal.substring(match.range.last + 1)
                    }
                }
                '\t' -> {
                    writer.raw("&#9;")
                    literal = literal.substring(1)
                }
                ' ' -> {
                    writer.raw("&#32;")
                    literal = literal.substring(1)
                }
            }
        }

        val escape = if (text.parent is Heading) textEscapeInHeading else textEscape
        if (literal.endsWith('!') && text.next is Link) {
            writer.text(literal.dropLast(1), escape)
            writer.raw("\\!")
        } else {
            writer.text(literal, escape)
        }
    }

    override fun visitChildren(parent: Node) {
        var node = parent.firstChild
        while (node != null) {
            val next = node.next
            context.render(node)
            node = next
        }
    }

    private fun writeLinkLike(
        title: String?,
        destination: String,
        node: Node,
        opener: String,
    ) {
        writer.raw(opener)
        visitChildren(node)
        writer.raw(']')
        writer.raw('(')
        if (contains(destination, linkDestinationNeedsAngleBrackets)) {
            writer.raw('<')
            writer.text(destination, linkDestinationEscapeInAngleBrackets)
            writer.raw('>')
        } else {
            writer.raw(destination)
        }
        if (title != null) {
            writer.raw(' ')
            writer.raw('"')
            writer.text(title, linkTitleEscapeInQuotes)
            writer.raw('"')
        }
        writer.raw(')')
    }

    private open class ListHolder(
        val parent: ListHolder?,
    )

    private class BulletListHolder(
        parent: ListHolder?,
        bulletList: BulletList,
    ) : ListHolder(parent) {
        val marker = bulletList.marker ?: "-"
    }

    private class OrderedListHolder(
        parent: ListHolder?,
        orderedList: OrderedList,
    ) : ListHolder(parent) {
        val delimiter = orderedList.markerDelimiter ?: "."
        var number = orderedList.markerStartNumber ?: 1
    }

    private class LineBreakVisitor : AbstractVisitor() {
        var hasLineBreak: Boolean = false
            private set

        override fun visit(softLineBreak: SoftLineBreak) {
            super.visit(softLineBreak)
            hasLineBreak = true
        }

        override fun visit(hardLineBreak: HardLineBreak) {
            super.visit(hardLineBreak)
            hasLineBreak = true
        }
    }

    companion object {
        private fun findMaxRunLength(
            needle: String,
            value: String,
        ): Int {
            var maxRunLength = 0
            var position = 0
            while (position < value.length) {
                position = value.indexOf(needle, position)
                if (position == -1) {
                    return maxRunLength
                }
                var runLength = 0
                do {
                    position += needle.length
                    runLength++
                } while (value.startsWith(needle, position))
                maxRunLength = maxOf(runLength, maxRunLength)
            }
            return maxRunLength
        }

        private fun contains(
            value: String,
            matcher: CharMatcher,
        ): Boolean =
            value.any(matcher::matches)

        private fun getLines(literal: String): List<String> {
            val parts = literal.split('\n')
            return if (parts.lastOrNull().isNullOrEmpty() && literal.endsWith('\n') || literal.isEmpty()) {
                parts.dropLast(1)
            } else {
                parts
            }
        }
    }
}
