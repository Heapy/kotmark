package io.heapy.kotmark.renderer.kotlinxhtml.adapters

import io.heapy.kotmark.ext.image.attributes.ImageAttributes
import io.heapy.kotmark.node.AbstractVisitor
import io.heapy.kotmark.node.CustomNode
import io.heapy.kotmark.node.Image
import io.heapy.kotmark.node.Node
import io.heapy.kotmark.renderer.NodeRenderer
import io.heapy.kotmark.renderer.html.AttributeProvider
import io.heapy.kotmark.renderer.kotlinxhtml.KotlinxHtmlAdapter

/** Merges image dimensions without unlinking metadata or otherwise mutating the AST. */
fun imageAttributesAdapter(): KotlinxHtmlAdapter = KotlinxHtmlAdapter { configuration ->
    configuration.attributeProviderFactory {
        AttributeProvider { node, tag, attributes ->
            if (node is Image && tag == "img") {
                node.accept(object : AbstractVisitor() {
                    override fun visit(customNode: CustomNode) {
                        if (customNode is ImageAttributes) attributes.putAll(customNode.attributes)
                        else super.visit(customNode)
                    }
                })
            }
        }
    }
    configuration.nodeRendererFactory {
        object : NodeRenderer {
            override val nodeTypes = setOf(ImageAttributes::class)
            override fun render(node: Node) = Unit
        }
    }
}
