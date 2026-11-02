package io.heapy.kotmark.ext.gfm.alerts.internal

import io.heapy.kotmark.ext.gfm.alerts.Alert
import io.heapy.kotmark.ext.gfm.alerts.AlertTitle
import io.heapy.kotmark.node.Node
import io.heapy.kotmark.renderer.text.LineBreakRendering
import io.heapy.kotmark.renderer.text.TextContentNodeRendererContext

class AlertTextContentNodeRenderer(
    private val context: TextContentNodeRendererContext,
    private val allowedTypes: Map<String, String>,
) : AlertNodeRenderer() {
    private val writer = context.writer

    override fun renderAlert(alert: Alert) {
        val defaultTitle =
            allowedTypes[alert.type]
                ?: error("Unknown alert type: ${alert.type}")
        val first = alert.firstChild
        if (first is AlertTitle) {
            renderChildren(first)
        } else {
            writer.write(defaultTitle)
        }
        if (alert.hasBody()) {
            if (context.lineBreakRendering == LineBreakRendering.STRIP) {
                writer.write(": ")
            } else {
                writer.block()
            }
            renderChildren(alert)
        }
        writer.block()
    }

    override fun renderNode(node: Node) {
        context.render(node)
    }
}
