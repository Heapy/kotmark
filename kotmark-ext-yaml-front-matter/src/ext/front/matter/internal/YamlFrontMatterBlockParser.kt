package io.heapy.kotmark.ext.front.matter.internal

import io.heapy.kotmark.ext.front.matter.YamlFrontMatterBlock
import io.heapy.kotmark.ext.front.matter.parser.FrontMatterParser
import io.heapy.kotmark.ext.front.matter.parser.YamlSubsetParser
import io.heapy.kotmark.node.Block
import io.heapy.kotmark.node.Document
import io.heapy.kotmark.parser.block.AbstractBlockParser
import io.heapy.kotmark.parser.block.AbstractBlockParserFactory
import io.heapy.kotmark.parser.block.BlockContinue
import io.heapy.kotmark.parser.block.BlockStart
import io.heapy.kotmark.parser.block.MatchedBlockParser
import io.heapy.kotmark.parser.block.ParserState

class YamlFrontMatterBlockParser(
    private val frontMatterParser: FrontMatterParser,
) : AbstractBlockParser() {
    constructor() : this(YamlSubsetParser())

    override val block: Block
        field = YamlFrontMatterBlock()

    override fun tryContinue(parserState: ParserState): BlockContinue {
        val line = parserState.line
        if (REGEX_END.matches(line.content)) {
            return when (frontMatterParser.onEndingSeparator(block, line)) {
                FrontMatterParser.SeparatorRole.BLOCK_END -> BlockContinue.Finished
                FrontMatterParser.SeparatorRole.CONTENT -> BlockContinue.AtIndex(parserState.index)
            }
        }

        frontMatterParser.onNextLine(block, line)
        return BlockContinue.AtIndex(parserState.index)
    }

    class Factory(
        private val frontMatterParserFactory: FrontMatterParser.Factory,
    ) : AbstractBlockParserFactory() {
        constructor() : this(YamlSubsetParser.Factory())

        override fun tryStart(
            state: ParserState,
            matchedBlockParser: MatchedBlockParser,
        ): BlockStart? {
            val parent = matchedBlockParser.matchedBlockParser.block
            return if (
                parent is Document &&
                parent.firstChild == null &&
                REGEX_BEGIN.matches(state.line.content)
            ) {
                BlockStart.of(YamlFrontMatterBlockParser(frontMatterParserFactory.create())).atIndex(state.nextNonSpaceIndex)
            } else {
                BlockStart.none()
            }
        }
    }

    companion object {
        private val REGEX_BEGIN = Regex("""^-{3}(\s.*)?""")
        private val REGEX_END = Regex("""^(-{3}|\.{3})(\s.*)?""")
    }
}
