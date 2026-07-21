package org.commonmark.parser

import org.commonmark.node.Node

fun interface PostProcessor {
    fun process(node: Node): Node
}
