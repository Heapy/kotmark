package io.heapy.kotmark.internal

import io.heapy.kotmark.node.Block
import io.heapy.kotmark.node.ListBlock
import io.heapy.kotmark.node.ListItem
import io.heapy.kotmark.node.Paragraph
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

    private var hadBlankLine: Boolean = false

    override fun isContainer(): Boolean = true

    override fun canContain(childBlock: Block): Boolean {
        if (hadBlankLine) {
            (block.parent as? ListBlock)?.isTight = false
        }
        return true
    }

    override fun tryContinue(parserState: ParserState): BlockContinue? {
        if (parserState.isBlank) {
            if (block.firstChild == null) {
                return BlockContinue.none()
            }
            val activeBlock = parserState.activeBlockParser.block
            hadBlankLine = activeBlock is Paragraph || activeBlock is ListItem
            return BlockContinue.atIndex(parserState.nextNonSpaceIndex)
        }

        return if (parserState.indent >= contentIndent) {
            BlockContinue.atColumn(parserState.column + contentIndent)
        } else {
            BlockContinue.none()
        }
    }
}
