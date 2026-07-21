package org.commonmark.ext.heading.anchor.internal

import org.commonmark.ext.heading.anchor.IdGenerator
import org.commonmark.node.AbstractVisitor
import org.commonmark.node.Code
import org.commonmark.node.Heading
import org.commonmark.node.Node
import org.commonmark.node.Text
import org.commonmark.renderer.html.AttributeProvider

class HeadingIdAttributeProvider private constructor(
    defaultId: String,
    prefix: String,
    suffix: String,
) : AttributeProvider {
    private val idGenerator =
        IdGenerator.builder()
            .defaultId(defaultId)
            .prefix(prefix)
            .suffix(suffix)
            .build()

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
