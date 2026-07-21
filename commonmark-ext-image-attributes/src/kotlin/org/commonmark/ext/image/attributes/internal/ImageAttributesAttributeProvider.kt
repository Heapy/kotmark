package org.commonmark.ext.image.attributes.internal

import org.commonmark.ext.image.attributes.ImageAttributes
import org.commonmark.node.AbstractVisitor
import org.commonmark.node.CustomNode
import org.commonmark.node.Image
import org.commonmark.node.Node
import org.commonmark.renderer.html.AttributeProvider

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
