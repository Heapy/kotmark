package io.heapy.kotmark.ext.footnotes.internal

import io.heapy.kotmark.ext.footnotes.FootnoteDefinition
import io.heapy.kotmark.node.Block
import io.heapy.kotmark.node.DefinitionMap
import io.heapy.kotmark.parser.block.AbstractBlockParser
import io.heapy.kotmark.parser.block.BlockContinue
import io.heapy.kotmark.parser.block.BlockParserFactory
import io.heapy.kotmark.parser.block.BlockStart
import io.heapy.kotmark.parser.block.MatchedBlockParser
import io.heapy.kotmark.parser.block.ParserState
import io.heapy.kotmark.text.Characters

class FootnoteBlockParser(
    label: String,
) : AbstractBlockParser() {
    private val definition = FootnoteDefinition(label)

    override val block: Block
        get() = definition

    override fun isContainer(): Boolean = true

    override fun canContain(childBlock: Block): Boolean = true

    override fun tryContinue(parserState: ParserState): BlockContinue? =
        when {
            parserState.indent >= 4 -> BlockContinue.atColumn(4)
            parserState.isBlank -> BlockContinue.atIndex(parserState.index)
            else -> BlockContinue.none()
        }

    override val definitions: List<DefinitionMap<*>>
        get() =
            listOf(
                DefinitionMap(FootnoteDefinition::class).apply {
                    putIfAbsent(definition.label, definition)
                },
            )

    class Factory : BlockParserFactory {
        override fun tryStart(
            state: ParserState,
            matchedBlockParser: MatchedBlockParser,
        ): BlockStart? {
            if (state.indent >= 4) {
                return BlockStart.none()
            }
            var index = state.nextNonSpaceIndex
            val content = state.line.content
            if (index >= content.length || content[index] != '[' || index + 1 >= content.length) {
                return BlockStart.none()
            }
            index++
            if (content[index] != '^' || index + 1 >= content.length) {
                return BlockStart.none()
            }
            index++
            val labelStart = index
            while (index < content.length) {
                when (content[index]) {
                    ']' ->
                        if (
                            index > labelStart &&
                            index + 1 < content.length &&
                            content[index + 1] == ':'
                        ) {
                            val label = content.subSequence(labelStart, index).toString()
                            val afterSpaces =
                                Characters.skipSpaceTab(content, index + 2, content.length)
                            return BlockStart.of(FootnoteBlockParser(label)).atIndex(afterSpaces)
                        } else {
                            return BlockStart.none()
                        }
                    ' ', '\r', '\n', '\u0000', '\t' -> return BlockStart.none()
                }
                index++
            }
            return BlockStart.none()
        }
    }
}
