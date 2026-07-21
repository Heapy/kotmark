package org.commonmark.parser.block

import org.commonmark.parser.SourceLines

interface MatchedBlockParser {
    val matchedBlockParser: BlockParser
    val paragraphLines: SourceLines
}
