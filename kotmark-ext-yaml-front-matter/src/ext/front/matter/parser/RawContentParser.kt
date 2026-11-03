package io.heapy.kotmark.ext.front.matter.parser

import io.heapy.kotmark.ext.front.matter.FrontMatterParser
import io.heapy.kotmark.ext.front.matter.FrontMatterParser.SeparatorRole
import io.heapy.kotmark.ext.front.matter.YamlFrontMatterBlock
import io.heapy.kotmark.ext.front.matter.YamlFrontMatterRawContent
import io.heapy.kotmark.parser.SourceLine

/** Keeps YAML lines as text so that callers can parse them with a YAML library of their choice. */
class RawContentParser : FrontMatterParser {
    private val content = StringBuilder()

    override fun onNextLine(block: YamlFrontMatterBlock, line: SourceLine) {
        content.append(line.content).append('\n')
    }

    override fun onEndingSeparator(block: YamlFrontMatterBlock, separator: SourceLine): SeparatorRole {
        block.appendChild(YamlFrontMatterRawContent(content.toString()))
        return SeparatorRole.BLOCK_END
    }

    class Factory : FrontMatterParser.Factory {
        override fun create(): FrontMatterParser = RawContentParser()
    }
}
