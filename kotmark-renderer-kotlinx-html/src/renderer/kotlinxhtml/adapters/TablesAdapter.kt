package io.heapy.kotmark.renderer.kotlinxhtml.adapters

import io.heapy.kotmark.ext.gfm.tables.*
import io.heapy.kotmark.renderer.kotlinxhtml.KotlinxHtmlAdapter
import io.heapy.kotmark.renderer.kotlinxhtml.nodeRenderer

fun tablesAdapter(): KotlinxHtmlAdapter = KotlinxHtmlAdapter { configuration ->
    configuration.nodeRenderer<TableBlock> { node -> element(node, "table") { renderChildren(node) } }
    configuration.nodeRenderer<TableHead> { node -> element(node, "thead") { renderChildren(node) } }
    configuration.nodeRenderer<TableBody> { node -> element(node, "tbody") { renderChildren(node) } }
    configuration.nodeRenderer<TableRow> { node -> element(node, "tr") { renderChildren(node) } }
    configuration.nodeRenderer<TableCell> { node ->
        val tag = if (node.isHeader) "th" else "td"
        val attrs = node.alignment?.let { mapOf("align" to it.name.lowercase()) }.orEmpty()
        element(node, tag, attrs) { renderChildren(node) }
    }
}
