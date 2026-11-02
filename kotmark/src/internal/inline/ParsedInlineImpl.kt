package io.heapy.kotmark.internal.inline

import io.heapy.kotmark.node.Node
import io.heapy.kotmark.parser.beta.ParsedInline
import io.heapy.kotmark.parser.beta.Position

class ParsedInlineImpl(
    val node: Node,
    val position: Position,
) : ParsedInline
