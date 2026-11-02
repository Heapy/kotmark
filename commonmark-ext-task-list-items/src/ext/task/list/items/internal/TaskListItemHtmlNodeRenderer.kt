package io.heapy.kotmark.ext.task.list.items.internal

import io.heapy.kotmark.ext.task.list.items.TaskListItemMarker
import io.heapy.kotmark.node.Node
import io.heapy.kotmark.renderer.html.HtmlNodeRendererContext

class TaskListItemHtmlNodeRenderer(
    private val context: HtmlNodeRendererContext,
) : TaskListItemNodeRenderer() {
    override fun render(node: Node) {
        val marker = node as TaskListItemMarker
        val attributes =
            linkedMapOf(
                "type" to "checkbox",
                "disabled" to "",
            )
        if (marker.isChecked) {
            attributes["checked"] = ""
        }
        context.writer.tag(
            "input",
            context.extendAttributes(node, "input", attributes),
        )
        context.writer.text(" ")
        renderChildren(node)
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
