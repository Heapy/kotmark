package io.heapy.kotmark.ext.front.matter

import io.heapy.kotmark.parser.block.BlockContinue

/** Receives the lines inside a YAML front matter block and appends its extracted nodes. */
interface YamlFrontMatterExtractor {
    fun onNextLine(block: YamlFrontMatterBlock, line: CharSequence)

    /** Called when a closing delimiter is encountered, not when input ends without one. */
    fun onBlockEnd(block: YamlFrontMatterBlock): BlockContinue

    /** Creates a fresh extractor for each front matter block; the factory itself is shared. */
    fun interface Factory {
        fun create(): YamlFrontMatterExtractor
    }
}
