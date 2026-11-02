package io.heapy.kotmark.internal.inline

import io.heapy.kotmark.node.Node
import io.heapy.kotmark.parser.beta.LinkResult
import io.heapy.kotmark.parser.beta.Position

class LinkResultImpl(
    val type: Type,
    val node: Node,
    val position: Position,
) : LinkResult {
    var isIncludeMarker: Boolean = false
        private set

    override fun includeMarker(): LinkResult = apply {
        isIncludeMarker = true
    }

    enum class Type {
        WRAP,
        REPLACE,
    }
}
