package io.heapy.kotmark.renderer.kotlinxhtml.adapters

import io.heapy.kotmark.ext.gfm.alerts.Alert
import io.heapy.kotmark.ext.gfm.alerts.AlertTitle
import io.heapy.kotmark.ext.gfm.alerts.AlertsExtension
import io.heapy.kotmark.node.Node
import io.heapy.kotmark.renderer.NodeRenderer
import io.heapy.kotmark.renderer.kotlinxhtml.KotlinxHtmlAdapter

/** Use the same type/title mapping as the parser when defining custom alert types. */
fun alertsAdapter(allowedTypes: Map<String, String> = AlertsExtension.STANDARD_TYPES): KotlinxHtmlAdapter {
    val titles = allowedTypes.toMap()
    require(titles.isNotEmpty() && titles.all { [type, title] -> type.isNotEmpty() && type == type.uppercase() && title.isNotEmpty() })
    return KotlinxHtmlAdapter { configuration ->
        configuration.nodeRendererFactory { context ->
            object : NodeRenderer {
                override val nodeTypes = setOf(Alert::class, AlertTitle::class)
                override fun render(node: Node) {
                    if (node !is Alert) return // Title is emitted once by its parent.
                    val title = titles[node.type] ?: error("Unknown alert type: ${node.type}")
                    val type = node.type.lowercase()
                    context.element(node, "div", linkedMapOf("class" to "markdown-alert markdown-alert-$type", "data-alert-type" to type)) {
                        context.element(node, "p", mapOf("class" to "markdown-alert-title")) {
                            val first = node.firstChild
                            if (first is AlertTitle) context.renderChildren(first) else context.text(title)
                        }
                        context.renderChildren(node)
                    }
                }
            }
        }
    }
}
