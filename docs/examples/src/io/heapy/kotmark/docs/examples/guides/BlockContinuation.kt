package io.heapy.kotmark.docs.examples.guides

// region block-continuation
import io.heapy.kotmark.node.CustomBlock
import io.heapy.kotmark.parser.block.AbstractBlockParser
import io.heapy.kotmark.parser.block.BlockContinue
import io.heapy.kotmark.parser.block.ParserState

class NoteBlock : CustomBlock()

class NoteBlockParser : AbstractBlockParser() {
    override val block = NoteBlock()

    override fun tryContinue(parserState: ParserState): BlockContinue? = when {
        parserState.line.content.toString() == "END" -> BlockContinue.Finished
        parserState.indent >= 4 -> BlockContinue.AtColumn(parserState.column + 4)
        parserState.isBlank -> BlockContinue.AtIndex(parserState.index)
        else -> null
    }
}
// endregion block-continuation
