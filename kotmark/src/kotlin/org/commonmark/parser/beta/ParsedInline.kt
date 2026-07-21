package org.commonmark.parser.beta

import org.commonmark.internal.inline.ParsedInlineImpl
import org.commonmark.node.Node
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
