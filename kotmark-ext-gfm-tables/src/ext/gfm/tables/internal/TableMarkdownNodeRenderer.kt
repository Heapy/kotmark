package io.heapy.kotmark.ext.gfm.tables.internal

import io.heapy.kotmark.ext.gfm.tables.TableBlock
import io.heapy.kotmark.ext.gfm.tables.TableBody
import io.heapy.kotmark.ext.gfm.tables.TableCell
import io.heapy.kotmark.ext.gfm.tables.TableHead
import io.heapy.kotmark.ext.gfm.tables.TableRow
import io.heapy.kotmark.node.Node
import io.heapy.kotmark.renderer.markdown.MarkdownNodeRendererContext
import io.heapy.kotmark.text.AsciiMatcher

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
