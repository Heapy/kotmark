package io.heapy.kotmark.ext.front.matter.parser

import io.heapy.kotmark.ext.front.matter.FrontMatterParser
import io.heapy.kotmark.ext.front.matter.FrontMatterParser.SeparatorRole
import io.heapy.kotmark.ext.front.matter.YamlFrontMatterBlock
import io.heapy.kotmark.ext.front.matter.YamlFrontMatterNode
import io.heapy.kotmark.parser.SourceLine

/** Parses the existing limited YAML subset into key/value nodes. */
class YamlSubsetParser : FrontMatterParser {
    private var inLiteral = false
    private var currentKey: String? = null
    private var currentValues = mutableListOf<String>()

    override fun onNextLine(block: YamlFrontMatterBlock, line: SourceLine) {
        val metadata = REGEX_METADATA.matchEntire(line.content)
        if (metadata != null) {
            appendCurrentNode(block)
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
            val literal = REGEX_METADATA_LITERAL.matchEntire(line.content)
            if (literal != null) {
                val value = literal.groupValues[1].trim()
                if (currentValues.size == 1) {
                    currentValues[0] = "${currentValues[0]}\n$value"
                } else {
                    currentValues.add(value)
                }
            }
        } else {
            val listItem = REGEX_METADATA_LIST.matchEntire(line.content)
            if (listItem != null) {
                currentValues.add(parseString(listItem.groupValues[1]))
            }
        }
    }

    override fun onEndingSeparator(block: YamlFrontMatterBlock, separator: SourceLine): SeparatorRole {
        appendCurrentNode(block)
        return SeparatorRole.BLOCK_END
    }

    private fun appendCurrentNode(block: YamlFrontMatterBlock) {
        currentKey?.let { block.appendChild(YamlFrontMatterNode(it, currentValues)) }
    }

    class Factory : FrontMatterParser.Factory {
        override fun create(): FrontMatterParser = YamlSubsetParser()
    }

    private companion object {
        private val REGEX_METADATA = Regex("""^[ ]{0,3}([A-Za-z0-9._-]+):\s*(.*)""")
        private val REGEX_METADATA_LIST = Regex("""^[ ]+-\s*(.*)""")
        private val REGEX_METADATA_LITERAL = Regex("""^\s*(.*)""")

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
