package io.heapy.kotmark.ext.gfm.tables.internal

import io.heapy.kotmark.ext.gfm.tables.TableBlock
import io.heapy.kotmark.ext.gfm.tables.TableBody
import io.heapy.kotmark.ext.gfm.tables.TableCell
import io.heapy.kotmark.ext.gfm.tables.TableHead
import io.heapy.kotmark.ext.gfm.tables.TableRow
import io.heapy.kotmark.node.Node
import io.heapy.kotmark.renderer.text.TextContentNodeRendererContext

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
