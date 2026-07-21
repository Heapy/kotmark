package org.commonmark.ext.gfm.tables.internal

import org.commonmark.ext.gfm.tables.TableBlock
import org.commonmark.ext.gfm.tables.TableBody
import org.commonmark.ext.gfm.tables.TableCell
import org.commonmark.ext.gfm.tables.TableHead
import org.commonmark.ext.gfm.tables.TableRow
import org.commonmark.node.Node
import org.commonmark.renderer.text.TextContentNodeRendererContext

class TableTextContentNodeRenderer(
    private val context: TextContentNodeRendererContext,
) : TableNodeRenderer() {
    private val writer = context.writer

    override fun renderBlock(node: TableBlock) {
        writer.pushTight(true)
        renderChildren(node)
        writer.popTight()
        writer.block()
    }

    override fun renderHead(node: TableHead) {
        renderChildren(node)
    }

    override fun renderBody(node: TableBody) {
        renderChildren(node)
    }

    override fun renderRow(node: TableRow) {
        renderChildren(node)
        writer.block()
    }

    override fun renderCell(node: TableCell) {
        renderChildren(node)
        if (node.next != null) {
            writer.write('|')
            writer.whitespace()
        }
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
