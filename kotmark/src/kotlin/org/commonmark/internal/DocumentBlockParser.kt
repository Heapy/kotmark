package org.commonmark.internal

import org.commonmark.node.Block
import org.commonmark.node.Document
import org.commonmark.parser.block.AbstractBlockParser
import org.commonmark.parser.block.BlockContinue
import org.commonmark.parser.block.ParserState

class DocumentBlockParser : AbstractBlockParser() {
    override val block: Document = Document()

    override fun isContainer(): Boolean = true

    override fun canContain(childBlock: Block): Boolean = true

    override fun tryContinue(parserState: ParserState): BlockContinue =
        BlockContinue.atIndex(parserState.index)
}
