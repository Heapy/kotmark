package org.commonmark.ext.task.list.items.internal

import org.commonmark.ext.task.list.items.TaskListItemMarker
import org.commonmark.node.Node
import org.commonmark.renderer.NodeRenderer
import kotlin.reflect.KClass

abstract class TaskListItemNodeRenderer : NodeRenderer {
    final override val nodeTypes: Set<KClass<out Node>> = setOf(TaskListItemMarker::class)
}
