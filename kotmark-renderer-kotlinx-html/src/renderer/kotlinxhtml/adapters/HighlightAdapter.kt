package io.heapy.kotmark.renderer.kotlinxhtml.adapters

import io.heapy.kotmark.ext.highlight.Highlight
import io.heapy.kotmark.node.Node
import io.heapy.kotmark.renderer.NodeRenderer
import io.heapy.kotmark.renderer.kotlinxhtml.KotlinxHtmlAdapter

fun highlightAdapter(): KotlinxHtmlAdapter = KotlinxHtmlAdapter { configuration ->
    configuration.nodeRendererFactory { context ->
        object : NodeRenderer {
            override val nodeTypes = setOf(Highlight::class)
            override fun render(node: Node) {
                context.element(node, "mark") { context.renderChildren(node) }
            }
        }
    }
}
