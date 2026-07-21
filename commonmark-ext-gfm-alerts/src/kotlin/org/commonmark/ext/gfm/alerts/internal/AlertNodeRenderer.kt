package org.commonmark.ext.gfm.alerts.internal

import org.commonmark.ext.gfm.alerts.Alert
import org.commonmark.ext.gfm.alerts.AlertTitle
import org.commonmark.node.Node
import org.commonmark.renderer.NodeRenderer
import kotlin.reflect.KClass

abstract class AlertNodeRenderer : NodeRenderer {
    override val nodeTypes: Set<KClass<out Node>> = setOf(Alert::class)

    final override fun render(node: Node) {
        renderAlert(node as Alert)
    }

    protected abstract fun renderAlert(alert: Alert)

    protected fun renderChildren(parent: Node) {
        var child = parent.firstChild
        while (child != null) {
            val next = child.next
            if (child !is AlertTitle) {
                renderNode(child)
            }
            child = next
        }
    }

    protected abstract fun renderNode(node: Node)
}
