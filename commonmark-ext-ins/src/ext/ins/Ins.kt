package io.heapy.kotmark.ext.ins

import io.heapy.kotmark.node.CustomNode
import io.heapy.kotmark.node.Delimited

class Ins : CustomNode(), Delimited {
    override val openingDelimiter: String = "++"
    override val closingDelimiter: String = "++"
}
