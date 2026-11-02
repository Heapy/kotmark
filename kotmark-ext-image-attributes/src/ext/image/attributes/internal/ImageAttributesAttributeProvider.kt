package io.heapy.kotmark.ext.image.attributes.internal

import io.heapy.kotmark.ext.image.attributes.ImageAttributes
import io.heapy.kotmark.node.AbstractVisitor
import io.heapy.kotmark.node.CustomNode
import io.heapy.kotmark.node.Image
import io.heapy.kotmark.node.Node
import io.heapy.kotmark.renderer.html.AttributeProvider

class ImageAttributesAttributeProvider private constructor() : AttributeProvider {
    override fun setAttributes(
        node: Node,
        tagName: String,
        attributes: MutableMap<String, String?>,
    ) {
        if (node !is Image) {
            return
        }
        node.accept(
            object : AbstractVisitor() {
                override fun visit(customNode: CustomNode) {
                    if (customNode is ImageAttributes) {
                        attributes.putAll(customNode.attributes)
                        customNode.unlink()
                    }
                }
            },
        )
    }

    companion object {
        fun create(): ImageAttributesAttributeProvider = ImageAttributesAttributeProvider()
    }
}
