package org.commonmark.internal.inline

import org.commonmark.node.Node
import org.commonmark.parser.beta.LinkResult
import org.commonmark.parser.beta.Position

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
