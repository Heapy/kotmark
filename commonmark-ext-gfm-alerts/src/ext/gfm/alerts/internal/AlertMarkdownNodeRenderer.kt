package io.heapy.kotmark.ext.gfm.alerts.internal

import io.heapy.kotmark.ext.gfm.alerts.Alert
import io.heapy.kotmark.ext.gfm.alerts.AlertTitle
import io.heapy.kotmark.node.Node
import io.heapy.kotmark.renderer.markdown.MarkdownNodeRendererContext

class AlertMarkdownNodeRenderer(
    private val context: MarkdownNodeRendererContext,
) : AlertNodeRenderer() {
    private val writer = context.writer

    override fun renderAlert(alert: Alert) {
        writer.writePrefix("> ")
        writer.pushPrefix("> ")
        writer.raw("[!${alert.type}]")
        val first = alert.firstChild
        if (first is AlertTitle) {
            writer.raw(" ")
            renderChildren(first)
        }
        if (alert.hasBody()) {
            writer.line()
            renderChildren(alert)
        }
        writer.popPrefix()
        writer.block()
    }

    override fun renderNode(node: Node) {
        context.render(node)
    }
}
