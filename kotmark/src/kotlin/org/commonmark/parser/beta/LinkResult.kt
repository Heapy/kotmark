package org.commonmark.parser.beta

import org.commonmark.internal.inline.LinkResultImpl
import org.commonmark.node.Node
import kotlin.jvm.JvmStatic

interface LinkResult {
    fun includeMarker(): LinkResult

    companion object {
        @JvmStatic
        fun none(): LinkResult? = null

        @JvmStatic
        fun wrapTextIn(node: Node, position: Position): LinkResult =
            LinkResultImpl(LinkResultImpl.Type.WRAP, node, position)

        @JvmStatic
        fun replaceWith(node: Node, position: Position): LinkResult =
            LinkResultImpl(LinkResultImpl.Type.REPLACE, node, position)
    }
}
