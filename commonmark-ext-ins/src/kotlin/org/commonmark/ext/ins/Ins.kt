package org.commonmark.ext.ins

import org.commonmark.node.CustomNode
import org.commonmark.node.Delimited

class Ins : CustomNode(), Delimited {
    override val openingDelimiter: String = "++"
    override val closingDelimiter: String = "++"
}
