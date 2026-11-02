package io.heapy.kotmark.parser

import io.heapy.kotmark.node.Node

/**
 * Parser for inline content such as text, links, and emphasized text.
 */
fun interface InlineParser {
    fun parse(lines: SourceLines, node: Node)
}
