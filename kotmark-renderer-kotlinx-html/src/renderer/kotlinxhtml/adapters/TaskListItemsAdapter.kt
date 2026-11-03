package io.heapy.kotmark.renderer.kotlinxhtml.adapters

import io.heapy.kotmark.ext.task.list.items.TaskListItemMarker
import io.heapy.kotmark.renderer.kotlinxhtml.KotlinxHtmlAdapter
import io.heapy.kotmark.renderer.kotlinxhtml.nodeRenderer

fun taskListItemsAdapter(): KotlinxHtmlAdapter = KotlinxHtmlAdapter { configuration ->
    configuration.nodeRenderer<TaskListItemMarker> { marker ->
        val attributes = linkedMapOf("type" to "checkbox", "disabled" to "")
        if (marker.isChecked) attributes["checked"] = ""
        element(marker, "input", attributes)
        text(" ")
    }
}
