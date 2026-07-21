package org.commonmark.ext.task.list.items.internal

import org.commonmark.ext.task.list.items.TaskListItemMarker
import org.commonmark.node.AbstractVisitor
import org.commonmark.node.ListItem
import org.commonmark.node.Node
import org.commonmark.node.Paragraph
import org.commonmark.node.Text
import org.commonmark.parser.PostProcessor

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
        private val REGEX_TASK_LIST_ITEM = Regex("^\\[([xX\\s])]\\s+(.*)")
    }
}
