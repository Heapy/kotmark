package io.heapy.kotmark.ext.gfm.strikethrough

import io.heapy.kotmark.node.CustomNode
import io.heapy.kotmark.node.Delimited

class Strikethrough(
    private val delimiter: String,
) : CustomNode(), Delimited {
    override val openingDelimiter: String = delimiter
    override val closingDelimiter: String = delimiter
}
