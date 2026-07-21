package org.commonmark.ext.task.list.items.internal

import org.commonmark.ext.task.list.items.TaskListItemMarker
import org.commonmark.node.Node
import org.commonmark.renderer.markdown.MarkdownNodeRendererContext

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
