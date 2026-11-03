package io.heapy.kotmark.internal

import io.heapy.kotmark.node.Block
import io.heapy.kotmark.node.ListBlock
import io.heapy.kotmark.node.ListItem
import io.heapy.kotmark.parser.block.AbstractBlockParser
import io.heapy.kotmark.parser.block.BlockContinue
import io.heapy.kotmark.parser.block.ParserState

class ListItemParser(
    markerIndent: Int,
    private val contentIndent: Int,
) : AbstractBlockParser() {
    override val block: ListItem = ListItem().apply {
        this.markerIndent = markerIndent
        this.contentIndent = this@ListItemParser.contentIndent
    }

    private var lineBlank: Boolean = false
    private var previousLineBlank: Boolean = false

    override fun isContainer(): Boolean = true

    override fun canContain(childBlock: Block): Boolean {
        if (previousLineBlank) {
            // A new block after a blank line makes the list loose; an empty item would have ended.
            (block.parent as? ListBlock)?.isTight = false
        }
        return true
    }

    override fun tryContinue(parserState: ParserState): BlockContinue? {
        // Blank lines inside a continuing block must not affect the list's tightness later.
        previousLineBlank = lineBlank
        lineBlank = parserState.isBlank
        if (lineBlank) {
            if (block.firstChild == null) {
                return null
            }
            return BlockContinue.AtIndex(parserState.nextNonSpaceIndex)
        }

        return if (parserState.indent >= contentIndent) {
            BlockContinue.AtColumn(parserState.column + contentIndent)
        } else {
            null
        }
    }
}
