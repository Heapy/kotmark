package io.heapy.kotmark.ext.toc

import io.heapy.kotmark.node.CustomBlock

/** A TOC insertion point. Markdown rendering preserves [marker]; generated entries are not AST children. */
class TableOfContents(val marker: String = "[TOC]") : CustomBlock()

/** Where to insert a TOC when the source has no explicit marker. */
enum class TocPlacement {
    /** Only explicit markers produce a TOC. */
    MARKERS,
    /** Insert at the beginning of the document if no marker exists. */
    PREPEND_IF_MISSING,
    /** Insert at the end of the document if no marker exists. */
    APPEND_IF_MISSING,
}
