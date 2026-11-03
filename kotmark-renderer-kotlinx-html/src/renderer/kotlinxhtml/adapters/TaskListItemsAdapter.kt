package io.heapy.kotmark.renderer.kotlinxhtml.adapters

import io.heapy.kotmark.ext.task.list.items.TaskListItemMarker
import io.heapy.kotmark.node.Node
import io.heapy.kotmark.renderer.NodeRenderer
import io.heapy.kotmark.renderer.kotlinxhtml.KotlinxHtmlAdapter

fun taskListItemsAdapter(): KotlinxHtmlAdapter = KotlinxHtmlAdapter { configuration ->
    configuration.nodeRendererFactory { context ->
        object : NodeRenderer {
            override val nodeTypes = setOf(TaskListItemMarker::class)
            override fun render(node: Node) {
                val marker = node as TaskListItemMarker
                val attributes = linkedMapOf("type" to "checkbox", "disabled" to "")
                if (marker.isChecked) attributes["checked"] = ""
                context.element(node, "input", attributes)
                context.text(" ")
            }
        }
    }
}
