package io.heapy.kotmark.renderer.kotlinxhtml.adapters

import io.heapy.kotmark.ext.math.InlineMath
import io.heapy.kotmark.ext.math.MathBlock
import io.heapy.kotmark.node.Node
import io.heapy.kotmark.renderer.NodeRenderer
import io.heapy.kotmark.renderer.kotlinxhtml.KotlinxHtmlAdapter

/** Emits escaped TeX source in math containers. Typesetting/KaTeX assets belong to the application. */
fun mathAdapter(): KotlinxHtmlAdapter = KotlinxHtmlAdapter { configuration ->
    configuration.nodeRendererFactory { context ->
        object : NodeRenderer {
            override val nodeTypes = setOf(InlineMath::class, MathBlock::class)
            override fun render(node: Node) {
                when (node) {
                    is InlineMath -> context.element(node, "span", mapOf("class" to "math math-inline")) { context.text(node.literal) }
                    is MathBlock -> context.element(node, "div", mapOf("class" to "math math-block")) { context.text(node.literal) }
                }
            }
        }
    }
}
