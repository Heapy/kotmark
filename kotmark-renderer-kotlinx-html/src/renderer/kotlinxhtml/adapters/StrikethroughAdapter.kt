package io.heapy.kotmark.renderer.kotlinxhtml.adapters

import io.heapy.kotmark.ext.gfm.strikethrough.Strikethrough
import io.heapy.kotmark.node.Node
import io.heapy.kotmark.renderer.NodeRenderer
import io.heapy.kotmark.renderer.kotlinxhtml.KotlinxHtmlAdapter

fun strikethroughAdapter(): KotlinxHtmlAdapter = KotlinxHtmlAdapter { configuration ->
    configuration.nodeRendererFactory { context ->
        object : NodeRenderer {
            override val nodeTypes = setOf(Strikethrough::class)
            override fun render(node: Node) {
                context.element(node, "del") { context.renderChildren(node) }
            }
        }
    }
}
