package io.heapy.kotmark.ext.math

import io.heapy.kotmark.node.CustomNode
import io.heapy.kotmark.node.Delimited

/** An opaque formula: [literal] excludes the delimiters and is not Markdown-unescaped. */
class InlineMath(var literal: String = "") : CustomNode(), Delimited {
    override val openingDelimiter: String = "$"
    override val closingDelimiter: String = "$"
}
