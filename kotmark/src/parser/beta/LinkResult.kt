package io.heapy.kotmark.parser.beta

import io.heapy.kotmark.internal.inline.LinkResultImpl
import io.heapy.kotmark.node.Node
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
