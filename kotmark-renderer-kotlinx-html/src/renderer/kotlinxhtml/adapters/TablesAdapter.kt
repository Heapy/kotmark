package io.heapy.kotmark.renderer.kotlinxhtml.adapters

import io.heapy.kotmark.ext.gfm.tables.*
import io.heapy.kotmark.node.Node
import io.heapy.kotmark.renderer.NodeRenderer
import io.heapy.kotmark.renderer.kotlinxhtml.KotlinxHtmlAdapter

fun tablesAdapter(): KotlinxHtmlAdapter = KotlinxHtmlAdapter { configuration ->
    configuration.nodeRendererFactory { context ->
        object : NodeRenderer {
            override val nodeTypes = setOf(TableBlock::class, TableHead::class, TableBody::class, TableRow::class, TableCell::class)
            override fun render(node: Node) {
                val tag = when (node) {
                    is TableBlock -> "table"
                    is TableHead -> "thead"
                    is TableBody -> "tbody"
                    is TableRow -> "tr"
                    is TableCell -> if (node.isHeader) "th" else "td"
                    else -> error("Not a table node: ${node::class}")
                }
                val attrs = (node as? TableCell)?.alignment?.let { mapOf("align" to it.name.lowercase()) }.orEmpty()
                context.element(node, tag, attrs) { context.renderChildren(node) }
            }
        }
    }
}
