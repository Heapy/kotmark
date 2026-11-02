package io.heapy.kotmark.parser

import io.heapy.kotmark.node.Node

fun interface PostProcessor {
    fun process(node: Node): Node
}
