package io.heapy.kotmark.ext.heading.anchor.internal

import io.heapy.kotmark.ext.heading.anchor.idGenerator
import io.heapy.kotmark.node.AbstractVisitor
import io.heapy.kotmark.node.Code
import io.heapy.kotmark.node.Heading
import io.heapy.kotmark.node.Node
import io.heapy.kotmark.node.Text
import io.heapy.kotmark.renderer.html.AttributeProvider

class HeadingIdAttributeProvider private constructor(
    defaultId: String,
    prefix: String,
    suffix: String,
) : AttributeProvider {
    private val idGenerator =
        idGenerator {
            this.defaultId = defaultId
            this.prefix = prefix
            this.suffix = suffix
        }

    override fun setAttributes(
        node: Node,
        tagName: String,
        attributes: MutableMap<String, String?>,
    ) {
        if (node !is Heading) {
            return
        }
        val headingText = StringBuilder()
        node.accept(
            object : AbstractVisitor() {
                override fun visit(text: Text) {
                    headingText.append(text.literal)
                }

                override fun visit(code: Code) {
                    headingText.append(code.literal)
                }
            },
        )
        attributes["id"] = idGenerator.generateId(headingText.toString().trim().lowercase())
    }

    companion object {
        fun create(
            defaultId: String,
            prefix: String,
            suffix: String,
        ): HeadingIdAttributeProvider =
            HeadingIdAttributeProvider(defaultId, prefix, suffix)
    }
}
