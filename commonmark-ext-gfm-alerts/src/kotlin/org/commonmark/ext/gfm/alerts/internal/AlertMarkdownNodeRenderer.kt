package org.commonmark.ext.gfm.alerts.internal

import org.commonmark.ext.gfm.alerts.Alert
import org.commonmark.ext.gfm.alerts.AlertTitle
import org.commonmark.node.Node
import org.commonmark.renderer.markdown.MarkdownNodeRendererContext

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
