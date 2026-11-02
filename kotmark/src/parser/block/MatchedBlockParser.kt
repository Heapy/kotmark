package io.heapy.kotmark.parser.block

import io.heapy.kotmark.parser.SourceLines

interface MatchedBlockParser {
    val matchedBlockParser: BlockParser
    val paragraphLines: SourceLines
}
