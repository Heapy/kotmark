package io.heapy.kotmark.ext.task.list.items.internal

import io.heapy.kotmark.ext.task.list.items.TaskListItemMarker
import io.heapy.kotmark.node.Node
import io.heapy.kotmark.renderer.markdown.MarkdownNodeRendererContext

class TaskListItemMarkdownNodeRenderer(
    private val context: MarkdownNodeRendererContext,
) : TaskListItemNodeRenderer() {
    override fun render(node: Node) {
        val marker = node as TaskListItemMarker
        context.writer.raw(if (marker.isChecked) "[x] " else "[ ] ")
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
