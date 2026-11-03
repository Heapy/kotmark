package io.heapy.kotmark.ext.front.matter.internal

import io.heapy.kotmark.ext.front.matter.YamlFrontMatterBlock
import io.heapy.kotmark.ext.front.matter.YamlFrontMatterExtractor
import io.heapy.kotmark.ext.front.matter.extractor.YamlDataExtractor
import io.heapy.kotmark.node.Block
import io.heapy.kotmark.node.Document
import io.heapy.kotmark.parser.block.AbstractBlockParser
import io.heapy.kotmark.parser.block.AbstractBlockParserFactory
import io.heapy.kotmark.parser.block.BlockContinue
import io.heapy.kotmark.parser.block.BlockStart
import io.heapy.kotmark.parser.block.MatchedBlockParser
import io.heapy.kotmark.parser.block.ParserState

class YamlFrontMatterBlockParser(
    private val extractor: YamlFrontMatterExtractor,
) : AbstractBlockParser() {
    constructor() : this(YamlDataExtractor())

    override val block: Block
        field = YamlFrontMatterBlock()

    override fun tryContinue(parserState: ParserState): BlockContinue {
        val line = parserState.line.content
        if (REGEX_END.matches(line)) {
            return extractor.onBlockEnd(block)
        }

        extractor.onNextLine(block, line)
        return BlockContinue.AtIndex(parserState.index)
    }

    class Factory(
        private val extractorFactory: YamlFrontMatterExtractor.Factory,
    ) : AbstractBlockParserFactory() {
        constructor() : this(YamlDataExtractor.Factory())

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
                BlockStart.of(YamlFrontMatterBlockParser(extractorFactory.create())).atIndex(state.nextNonSpaceIndex)
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
