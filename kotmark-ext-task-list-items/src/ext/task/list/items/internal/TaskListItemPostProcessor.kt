package io.heapy.kotmark.ext.task.list.items.internal

import io.heapy.kotmark.ext.task.list.items.TaskListItemMarker
import io.heapy.kotmark.node.AbstractVisitor
import io.heapy.kotmark.node.ListItem
import io.heapy.kotmark.node.Node
import io.heapy.kotmark.node.Paragraph
import io.heapy.kotmark.node.Text
import io.heapy.kotmark.parser.PostProcessor

class TaskListItemPostProcessor : PostProcessor {
    override fun process(node: Node): Node {
        node.accept(TaskListItemVisitor())
        return node
    }

    private class TaskListItemVisitor : AbstractVisitor() {
        override fun visit(listItem: ListItem) {
            val paragraph = listItem.firstChild as? Paragraph
            val text = paragraph?.firstChild as? Text
            val match = text?.literal?.let(REGEX_TASK_LIST_ITEM::matchEntire)
            if (match != null) {
                listItem.prependChild(
                    TaskListItemMarker(match.groupValues[1].equals("x", ignoreCase = true)),
                )
                text.literal = match.groupValues[2]
            }
            visitChildren(listItem)
        }
    }

    companion object {
        private val REGEX_TASK_LIST_ITEM = Regex("""^\[([xX\s])\]\s+(.*)""")
    }
}
