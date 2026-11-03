package io.heapy.kotmark.renderer.kotlinxhtml.adapters

import io.heapy.kotmark.ext.front.matter.YamlFrontMatterBlock
import io.heapy.kotmark.ext.front.matter.YamlFrontMatterNode
import io.heapy.kotmark.node.Node
import io.heapy.kotmark.renderer.NodeRenderer
import io.heapy.kotmark.renderer.kotlinxhtml.KotlinxHtmlAdapter

/** Front matter is document metadata and has no HTML representation. */
fun yamlFrontMatterAdapter(): KotlinxHtmlAdapter = KotlinxHtmlAdapter { configuration ->
    configuration.nodeRendererFactory {
        object : NodeRenderer {
            override val nodeTypes = setOf(YamlFrontMatterBlock::class, YamlFrontMatterNode::class)
            override fun render(node: Node) = Unit
        }
    }
}
