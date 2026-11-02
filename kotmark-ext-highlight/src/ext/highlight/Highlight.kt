package io.heapy.kotmark.ext.highlight

import io.heapy.kotmark.node.CustomNode
import io.heapy.kotmark.node.Delimited

class Highlight : CustomNode(), Delimited {
    override val openingDelimiter: String = "=="
    override val closingDelimiter: String = "=="
}
