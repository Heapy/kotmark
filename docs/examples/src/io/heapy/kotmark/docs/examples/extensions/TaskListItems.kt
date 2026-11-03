package io.heapy.kotmark.docs.examples.extensions

// region task-list-items
import io.heapy.kotmark.ext.task.list.items.TaskListItemsExtension
import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.html.htmlRenderer

fun renderWithTaskLists(markdown: String): String {
    val extensions = listOf(TaskListItemsExtension.create())
    val document = parser { extensions(extensions) }.parse(markdown)
    return htmlRenderer { extensions(extensions) }.render(document)
}
// endregion task-list-items
