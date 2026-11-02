package io.heapy.kotmark.internal

import io.heapy.kotmark.parser.block.BlockParser
import io.heapy.kotmark.parser.block.BlockStart

class BlockStartImpl(
    val blockParsers: Array<out BlockParser>,
) : BlockStart() {
    var newIndex: Int = -1
        private set
    var newColumn: Int = -1
        private set
    var isReplaceActiveBlockParser: Boolean = false
        private set
    var replaceParagraphLines: Int = 0
        private set

    override fun atIndex(newIndex: Int): BlockStart = apply {
        this.newIndex = newIndex
    }

    override fun atColumn(newColumn: Int): BlockStart = apply {
        this.newColumn = newColumn
    }

    @Deprecated("Use replaceParagraphLines(Int.MAX_VALUE)")
    override fun replaceActiveBlockParser(): BlockStart = apply {
        isReplaceActiveBlockParser = true
    }

    override fun replaceParagraphLines(lines: Int): BlockStart = apply {
        require(lines >= 1) { "Lines must be >= 1" }
        replaceParagraphLines = lines
    }
}
