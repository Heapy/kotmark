package org.commonmark.ext.footnotes

import org.commonmark.node.CustomBlock

class FootnoteDefinition(
    val label: String,
) : CustomBlock()
