package io.heapy.kotmark.internal

import io.heapy.kotmark.node.Block
import io.heapy.kotmark.node.Document
import io.heapy.kotmark.parser.block.AbstractBlockParser
import io.heapy.kotmark.parser.block.BlockContinue
import io.heapy.kotmark.parser.block.ParserState

class DocumentBlockParser : AbstractBlockParser() {
    override val block: Document = Document()

    override fun isContainer(): Boolean = true

    override fun canContain(childBlock: Block): Boolean = true

    override fun tryContinue(parserState: ParserState): BlockContinue =
        BlockContinue.atIndex(parserState.index)
}
