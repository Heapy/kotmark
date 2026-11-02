package io.heapy.kotmark.ext.gfm.alerts.internal

import io.heapy.kotmark.ext.gfm.alerts.Alert
import io.heapy.kotmark.ext.gfm.alerts.AlertTitle
import io.heapy.kotmark.node.Node
import io.heapy.kotmark.renderer.html.HtmlNodeRendererContext

class AlertHtmlNodeRenderer(
    private val context: HtmlNodeRendererContext,
    private val allowedTypes: Map<String, String>,
) : AlertNodeRenderer() {
    private val html = context.writer

    override fun renderAlert(alert: Alert) {
        val defaultTitle =
            allowedTypes[alert.type]
                ?: error("Unknown alert type: ${alert.type}")
        val cssClass = alert.type.lowercase()

        html.line()
        val attributes =
            linkedMapOf(
                "class" to "markdown-alert markdown-alert-$cssClass",
                "data-alert-type" to cssClass,
            )
        html.tag("div", context.extendAttributes(alert, "div", attributes))
        html.line()
        html.tag(
            "p",
            context.extendAttributes(alert, "p", mapOf("class" to "markdown-alert-title")),
        )
        val first = alert.firstChild
        if (first is AlertTitle) {
            renderChildren(first)
        } else {
            html.text(defaultTitle)
        }
        html.tag("/p")
        html.line()
        renderChildren(alert)
        html.tag("/div")
        html.line()
    }

    override fun renderNode(node: Node) {
        context.render(node)
    }
}
