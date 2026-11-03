package io.heapy.kotmark.ext.front.matter.extractor

import io.heapy.kotmark.ext.front.matter.YamlFrontMatterBlock
import io.heapy.kotmark.ext.front.matter.YamlFrontMatterContent
import io.heapy.kotmark.ext.front.matter.YamlFrontMatterExtractor
import io.heapy.kotmark.parser.block.BlockContinue

/** Keeps YAML lines as text so that callers can parse them with a YAML library of their choice. */
class YamlContentExtractor : YamlFrontMatterExtractor {
    private val content = StringBuilder()

    override fun onNextLine(block: YamlFrontMatterBlock, line: CharSequence) {
        content.append(line).append('\n')
    }

    override fun onBlockEnd(block: YamlFrontMatterBlock): BlockContinue {
        block.appendChild(YamlFrontMatterContent(content.toString()))
        return BlockContinue.Finished
    }

    class Factory : YamlFrontMatterExtractor.Factory {
        override fun create(): YamlFrontMatterExtractor = YamlContentExtractor()
    }
}
