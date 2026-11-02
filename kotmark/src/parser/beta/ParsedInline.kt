package io.heapy.kotmark.parser.beta

import io.heapy.kotmark.internal.inline.ParsedInlineImpl
import io.heapy.kotmark.node.Node
import kotlin.jvm.JvmStatic

/**
 * Result of a single inline content parser.
 */
interface ParsedInline {
    companion object {
        @JvmStatic
        fun none(): ParsedInline? = null

        @JvmStatic
        fun of(node: Node, position: Position): ParsedInline =
            ParsedInlineImpl(node, position)
    }
}
