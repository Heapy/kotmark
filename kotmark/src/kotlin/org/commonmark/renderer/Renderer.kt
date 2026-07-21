package org.commonmark.renderer

import org.commonmark.node.Node

interface Renderer {
    fun render(node: Node, output: Appendable)

    fun render(node: Node): String
}
