package org.commonmark.ext.task.list.items

import org.commonmark.node.CustomNode

class TaskListItemMarker(
    val isChecked: Boolean,
) : CustomNode()
