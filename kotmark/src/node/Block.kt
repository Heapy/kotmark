package io.heapy.kotmark.node

/**
 * Block nodes such as paragraphs, list blocks, code blocks etc.
 */
abstract class Block : Node() {
    override fun setParent(parent: Node?) {
        require(parent is Block) { "Parent of block must also be block (can not be inline)" }
        super.setParent(parent)
    }
}
