package io.heapy.kotmark.ext.front.matter.internal

import io.heapy.kotmark.ext.front.matter.YamlFrontMatterBlock
import io.heapy.kotmark.ext.front.matter.YamlFrontMatterNode
import io.heapy.kotmark.node.Block
import io.heapy.kotmark.node.Document
import io.heapy.kotmark.parser.block.AbstractBlockParser
import io.heapy.kotmark.parser.block.AbstractBlockParserFactory
import io.heapy.kotmark.parser.block.BlockContinue
import io.heapy.kotmark.parser.block.BlockStart
import io.heapy.kotmark.parser.block.MatchedBlockParser
import io.heapy.kotmark.parser.block.ParserState

class YamlFrontMatterBlockParser : AbstractBlockParser() {
    private var inLiteral = false
    private var currentKey: String? = null
    private var currentValues = mutableListOf<String>()

    override val block: Block
        field = YamlFrontMatterBlock()

    override fun tryContinue(parserState: ParserState): BlockContinue {
        val line = parserState.line.content.toString()
        if (REGEX_END.matches(line)) {
            appendCurrentNode()
            return BlockContinue.finished()
        }

        val metadata = REGEX_METADATA.matchEntire(line)
        if (metadata != null) {
            appendCurrentNode()
            inLiteral = false
            currentKey = metadata.groupValues[1]
            currentValues = mutableListOf()
            val value = metadata.groupValues[2]
            if (value == "|") {
                inLiteral = true
            } else if (value.isNotEmpty()) {
                currentValues.add(parseString(value))
            }
        } else if (inLiteral) {
            val literal = REGEX_METADATA_LITERAL.matchEntire(line)
            if (literal != null) {
                val value = literal.groupValues[1].trim()
                if (currentValues.size == 1) {
                    currentValues[0] = "${currentValues[0]}\n$value"
                } else {
                    currentValues.add(value)
                }
            }
        } else {
            val listItem = REGEX_METADATA_LIST.matchEntire(line)
            if (listItem != null) {
                currentValues.add(parseString(listItem.groupValues[1]))
            }
        }
        return BlockContinue.atIndex(parserState.index)
    }

    private fun appendCurrentNode() {
        currentKey?.let { block.appendChild(YamlFrontMatterNode(it, currentValues)) }
    }

    class Factory : AbstractBlockParserFactory() {
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
                BlockStart.of(YamlFrontMatterBlockParser()).atIndex(state.nextNonSpaceIndex)
            } else {
                BlockStart.none()
            }
        }
    }

    companion object {
        private val REGEX_METADATA = Regex("""^[ ]{0,3}([A-Za-z0-9._-]+):\s*(.*)""")
        private val REGEX_METADATA_LIST = Regex("""^[ ]+-\s*(.*)""")
        private val REGEX_METADATA_LITERAL = Regex("""^\s*(.*)""")
        private val REGEX_BEGIN = Regex("""^-{3}(\s.*)?""")
        private val REGEX_END = Regex("""^(-{3}|\.{3})(\s.*)?""")

        private fun parseString(value: String): String =
            when {
                value.startsWith("'") && value.endsWith("'") ->
                    value.substring(1, value.length - 1).replace("''", "'")
                value.startsWith("\"") && value.endsWith("\"") ->
                    value.substring(1, value.length - 1)
                        .replace("""\"""", "\"")
                        .replace("""\\""", "\\")
                else -> value
            }
    }
}
