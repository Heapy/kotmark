package io.heapy.kotmark.ext.task.list.items.internal

import io.heapy.kotmark.ext.task.list.items.TaskListItemMarker
import io.heapy.kotmark.node.Node
import io.heapy.kotmark.renderer.NodeRenderer
import kotlin.reflect.KClass

abstract class TaskListItemNodeRenderer : NodeRenderer {
    final override val nodeTypes: Set<KClass<out Node>> = setOf(TaskListItemMarker::class)
}
