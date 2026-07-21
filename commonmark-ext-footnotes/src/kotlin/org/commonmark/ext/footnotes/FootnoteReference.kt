package org.commonmark.ext.footnotes

import org.commonmark.node.CustomNode

class FootnoteReference(
    val label: String,
) : CustomNode()
