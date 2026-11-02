package io.heapy.kotmark.ext.image.attributes

import io.heapy.kotmark.node.CustomNode
import io.heapy.kotmark.node.Delimited

class ImageAttributes(
    val attributes: Map<String, String>,
) : CustomNode(), Delimited {
    override val openingDelimiter: String = "{"
    override val closingDelimiter: String = "}"

    override fun toStringAttributes(): String = "imageAttributes=$attributes"
}
