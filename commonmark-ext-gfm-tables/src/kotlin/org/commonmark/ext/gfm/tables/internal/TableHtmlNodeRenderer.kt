package org.commonmark.ext.gfm.tables.internal

import org.commonmark.ext.gfm.tables.TableBlock
import org.commonmark.ext.gfm.tables.TableBody
import org.commonmark.ext.gfm.tables.TableCell
import org.commonmark.ext.gfm.tables.TableHead
import org.commonmark.ext.gfm.tables.TableRow
import org.commonmark.node.Node
import org.commonmark.renderer.html.HtmlNodeRendererContext

class TableHtmlNodeRenderer(
    private val context: HtmlNodeRendererContext,
) : TableNodeRenderer() {
    private val html = context.writer

    override fun renderBlock(node: TableBlock) {
        renderContainer(node, "table")
    }

    override fun renderHead(node: TableHead) {
        renderContainer(node, "thead")
    }

    override fun renderBody(node: TableBody) {
        renderContainer(node, "tbody")
    }

    override fun renderRow(node: TableRow) {
        renderContainer(node, "tr")
    }

    override fun renderCell(node: TableCell) {
        val tag = if (node.isHeader) "th" else "td"
        html.line()
        val attributes =
            node.alignment?.let { mapOf("align" to it.name.lowercase()) } ?: emptyMap()
        html.tag(tag, context.extendAttributes(node, tag, attributes))
        renderChildren(node)
        html.tag("/$tag")
        html.line()
    }

    private fun renderContainer(node: Node, tag: String) {
        html.line()
        html.tag(tag, context.extendAttributes(node, tag, emptyMap()))
        renderChildren(node)
        html.tag("/$tag")
        html.line()
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
