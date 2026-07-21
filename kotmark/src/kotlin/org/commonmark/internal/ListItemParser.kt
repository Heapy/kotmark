package org.commonmark.internal

import org.commonmark.node.Block
import org.commonmark.node.ListBlock
import org.commonmark.node.ListItem
import org.commonmark.node.Paragraph
import org.commonmark.parser.block.AbstractBlockParser
import org.commonmark.parser.block.BlockContinue
import org.commonmark.parser.block.ParserState

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
