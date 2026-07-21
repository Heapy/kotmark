package org.commonmark.ext.gfm.strikethrough

import org.commonmark.node.CustomNode
import org.commonmark.node.Delimited

class Strikethrough(
    private val delimiter: String,
) : CustomNode(), Delimited {
    override val openingDelimiter: String = delimiter
    override val closingDelimiter: String = delimiter
}
