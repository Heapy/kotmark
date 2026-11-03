package io.heapy.kotmark.renderer.kotlinxhtml.adapters

import io.heapy.kotmark.ext.ins.Ins
import io.heapy.kotmark.node.Node
import io.heapy.kotmark.renderer.NodeRenderer
import io.heapy.kotmark.renderer.kotlinxhtml.KotlinxHtmlAdapter

fun insAdapter(): KotlinxHtmlAdapter = KotlinxHtmlAdapter { configuration ->
    configuration.nodeRendererFactory { context ->
        object : NodeRenderer {
            override val nodeTypes = setOf(Ins::class)
            override fun render(node: Node) {
                context.element(node, "ins") { context.renderChildren(node) }
            }
        }
    }
}
