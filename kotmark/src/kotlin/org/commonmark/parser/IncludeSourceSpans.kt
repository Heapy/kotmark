package org.commonmark.parser

/**
 * Controls whether source positions are attached to parsed nodes.
 */
enum class IncludeSourceSpans {
    NONE,
    BLOCKS,
    BLOCKS_AND_INLINES,
}
