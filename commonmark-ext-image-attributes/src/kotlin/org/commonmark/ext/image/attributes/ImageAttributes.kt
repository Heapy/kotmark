package org.commonmark.ext.image.attributes

import org.commonmark.node.CustomNode
import org.commonmark.node.Delimited

class ImageAttributes(
    val attributes: Map<String, String>,
) : CustomNode(), Delimited {
    override val openingDelimiter: String = "{"
    override val closingDelimiter: String = "}"

    override fun toStringAttributes(): String = "imageAttributes=$attributes"
}
