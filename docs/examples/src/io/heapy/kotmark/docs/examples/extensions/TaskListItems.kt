package io.heapy.kotmark.docs.examples.extensions

// region task-list-items
import io.heapy.kotmark.ext.task.list.items.taskListItems
import io.heapy.kotmark.extensions
import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.html.htmlRenderer

fun renderWithTaskLists(markdown: String): String {
    val extensions = extensions { taskListItems() }
    val document = parser { extensions(extensions) }.parse(markdown)
    return htmlRenderer { extensions(extensions) }.render(document)
}
// endregion task-list-items
