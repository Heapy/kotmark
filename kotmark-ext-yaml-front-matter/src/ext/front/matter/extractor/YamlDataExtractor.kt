package io.heapy.kotmark.ext.front.matter.extractor

import io.heapy.kotmark.ext.front.matter.YamlFrontMatterBlock
import io.heapy.kotmark.ext.front.matter.YamlFrontMatterExtractor
import io.heapy.kotmark.ext.front.matter.YamlFrontMatterNode
import io.heapy.kotmark.parser.block.BlockContinue

/** Parses the existing limited YAML subset into key/value nodes. */
class YamlDataExtractor : YamlFrontMatterExtractor {
    private var inLiteral = false
    private var currentKey: String? = null
    private var currentValues = mutableListOf<String>()

    override fun onNextLine(block: YamlFrontMatterBlock, line: CharSequence) {
        val metadata = REGEX_METADATA.matchEntire(line)
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
    }

    override fun onBlockEnd(block: YamlFrontMatterBlock): BlockContinue {
        appendCurrentNode(block)
        return BlockContinue.Finished
    }

    private fun appendCurrentNode(block: YamlFrontMatterBlock) {
        currentKey?.let { block.appendChild(YamlFrontMatterNode(it, currentValues)) }
    }

    class Factory : YamlFrontMatterExtractor.Factory {
        override fun create(): YamlFrontMatterExtractor = YamlDataExtractor()
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
