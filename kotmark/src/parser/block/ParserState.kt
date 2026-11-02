package io.heapy.kotmark.parser.block

import io.heapy.kotmark.parser.SourceLine

interface ParserState {
    val line: SourceLine
    val index: Int
    val nextNonSpaceIndex: Int
    val column: Int
    val indent: Int
    val isBlank: Boolean
    val activeBlockParser: BlockParser
}
