package org.commonmark.ext.gfm.tables.internal

import org.commonmark.ext.gfm.tables.TableBlock
import org.commonmark.ext.gfm.tables.TableBody
import org.commonmark.ext.gfm.tables.TableCell
import org.commonmark.ext.gfm.tables.TableHead
import org.commonmark.ext.gfm.tables.TableRow
import org.commonmark.node.Node
import org.commonmark.renderer.markdown.MarkdownNodeRendererContext
import org.commonmark.text.AsciiMatcher

class TableMarkdownNodeRenderer(
    private val context: MarkdownNodeRendererContext,
) : TableNodeRenderer() {
    private val writer = context.writer
    private val pipe = AsciiMatcher.builder().c('|').build()
    private val columns = mutableListOf<TableCell.Alignment?>()

    override fun renderBlock(node: TableBlock) {
        columns.clear()
        writer.pushTight(true)
        renderChildren(node)
        writer.popTight()
        writer.block()
    }

    override fun renderHead(node: TableHead) {
        renderChildren(node)
        for (alignment in columns) {
            writer.raw('|')
            writer.raw(
                when (alignment) {
                    TableCell.Alignment.LEFT -> ":---"
                    TableCell.Alignment.RIGHT -> "---:"
                    TableCell.Alignment.CENTER -> ":---:"
                    null -> "---"
                },
            )
        }
        writer.raw("|")
        writer.block()
    }

    override fun renderBody(node: TableBody) {
        renderChildren(node)
    }

    override fun renderRow(node: TableRow) {
        renderChildren(node)
        writer.raw("|")
        writer.block()
    }

    override fun renderCell(node: TableCell) {
        if (node.parent?.parent is TableHead) {
            columns.add(node.alignment)
        }
        writer.raw("|")
        writer.pushRawEscape(pipe)
        renderChildren(node)
        writer.popRawEscape()
    }

    private fun renderChildren(parent: Node) {
        var child = parent.firstChild
        while (child != null) {
            val next = child.next
            context.render(child)
            child = next
        }
    }
}
