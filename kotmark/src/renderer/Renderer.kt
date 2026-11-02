package io.heapy.kotmark.renderer

import io.heapy.kotmark.node.Node

interface Renderer {
    fun render(node: Node, output: Appendable)

    fun render(node: Node): String
}
